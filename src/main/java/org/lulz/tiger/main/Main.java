package org.lulz.tiger.main;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.io.*;
import org.lulz.tiger.antlr.TigerLexer;
import org.lulz.tiger.antlr.TigerParser;
import org.lulz.tiger.antlr.TigerParserBaseListener;
import org.lulz.tiger.backend.cfg.ControlFlowAnalyzer;
import org.lulz.tiger.backend.codegen.MIPSCodeGenerator;
import org.lulz.tiger.backend.codegen.BriggsRegisterAllocator;
import org.lulz.tiger.backend.codegen.NaiveRegisterAllocator;
import org.lulz.tiger.backend.codegen.RegisterAllocator;
import org.lulz.tiger.backend.liveness.LivenessAnalyzer;
import org.lulz.tiger.backend.liveness.Web;
import org.lulz.tiger.common.ir.BasicBlock;
import org.lulz.tiger.common.ir.IRListing;
import org.lulz.tiger.common.symbol.SymbolTable;
import org.lulz.tiger.common.type.TypeManager;
import org.lulz.tiger.frontend.SemanticException;
import org.lulz.tiger.frontend.irgen.IRGenVisitor;
import org.lulz.tiger.frontend.semantic.StructuralCheckListener;
import org.lulz.tiger.frontend.semantic.TypeCheckListener;
import org.lulz.tiger.frontend.symbol.BindingAnalysisListener;
import org.lulz.tiger.frontend.symbol.SymbolListener;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main implements Callable<Integer> {
    @Parameters(description = "tiger source file")
    private Path file;

    @Option(names = "--tokens", description = "print token stream from scanner and parser")
    private boolean printTokens;

    @Option(names = "--symtab", description = "print symbol table after parse")
    private boolean printSymtab;

    @Option(names = "--dot-cfg", description = "emit CFG as 'dot' file for every function in program")
    private boolean dotCfg;

    @Option(names = "--dot-web", description = "emit web interference graph as 'dot' file for every function in program")
    private boolean dotWeb;

    @Option(names = "--cfg-liveness", description = "include liveness sets in CFG output")
    private boolean cfgLiveness;

    @Option(names = {"--out", "-o"}, description = "MIPS assembly output file")
    private Path outFile;

    @Option(names = "--run", description = "run SPIM simulator after compilation")
    private boolean runSPIM;

    @Option(names = "--ralloc", description = "register allocation mode (${COMPLETION-CANDIDATES})", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private RegisterAllocationMode rallocMode = RegisterAllocationMode.BRIGGS;

    @Override
    public Integer call() throws IOException, InterruptedException {
        if (cfgLiveness && !dotCfg) {
            System.out.println("Invalid flag: --cfg-liveness cannot be used without --dot-cfg");
            System.exit(-1);
        }

        TigerLexer lexer = new TigerLexer(CharStreams.fromPath(file));
        CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
        TigerParser parser = new TigerParser(commonTokenStream);

        TigerErrorListener errorListener = new TigerErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        // check for scanner errors
        commonTokenStream.fill();
        if (errorListener.hasError) {
            System.out.println("stopped due to scanner error");
            System.exit(-1);
        }
        if (printTokens) {
            System.out.println("[debug] scanner token stream:");
            commonTokenStream.getTokens().stream().map(t ->
                    "< " + lexer.getVocabulary().getSymbolicName(t.getType()) + ", " + t.getText() + " >")
                    .forEach(System.out::println);
        }

        // parsing
        TigerParser.TigerProgramContext programContext = parser.tigerProgram();

        ParseTreeWalker walker = new ParseTreeWalker();
        TigerDebugListener listener = new TigerDebugListener();
        walker.walk(listener, programContext);

        if (errorListener.hasError) {
            System.out.println("stopped due to parser error");
            System.exit(-1);
        }
        if (printTokens) {
            System.out.println("[debug] parser token stream:");
            Stream<String> parsedNodes = listener.nodes.stream()
                    .map(node -> lexer.getVocabulary().getSymbolicName(node.getSymbol().getType()));
            System.out.println(parsedNodes.collect(Collectors.joining(" ")));
        }
        System.out.println("successful parse");

        IRListing listing = null;
        // semantic analysis
        try {
            SymbolTable symtab = new SymbolTable();
            TypeManager typeManager = new TypeManager();
            walker.walk(new SymbolListener(symtab, typeManager), programContext);

            if (printSymtab) {
                System.out.println("[debug] symbol table:");
                System.out.println(symtab);
            }

            walker.walk(new BindingAnalysisListener(symtab, typeManager), programContext);
            walker.walk(new TypeCheckListener(symtab, typeManager), programContext);
            walker.walk(new StructuralCheckListener(symtab, typeManager), programContext);
            IRGenVisitor irVisitor = new IRGenVisitor(symtab, typeManager);
            irVisitor.visit(programContext);
            listing = irVisitor.getListing();
            System.out.println(irVisitor.getListing());

            System.out.println("successful compile\n");
        } catch (SemanticException e) {
            printSemanticError(e, commonTokenStream);
            System.out.println("stopped due to semantic error");
            System.exit(-1);
        }

        new ControlFlowAnalyzer(listing).run();
        if (dotCfg && !cfgLiveness) {   // export cfg without live sets
            exportCfg(listing);
        }
        new LivenessAnalyzer(listing).run();
        if (dotCfg && cfgLiveness) {    // export cfg with live sets
            exportCfg(listing);
        }
        if (dotWeb) {
            exportWeb(listing);
        }


        System.out.println("MIPS Assembly:");
        RegisterAllocator regAlloc;
        if (rallocMode == RegisterAllocationMode.BRIGGS) {
            regAlloc = new BriggsRegisterAllocator();
        } else {
            regAlloc = new NaiveRegisterAllocator();
        }
        String code = new MIPSCodeGenerator(listing, regAlloc).call();
        System.out.print(code);

        if (outFile != null) {
            Files.writeString(outFile, code);
        }

        if (runSPIM) {
            System.out.println("\nStarting SPIM simulator");
            if (outFile == null) {
                outFile = Files.createTempFile(null, null);
                Files.writeString(outFile, code);
            }
            ProcessBuilder pb = new ProcessBuilder("spim", "-file", outFile.toString());
            pb.inheritIO();
            pb.start().waitFor();
        }
        return 0;
    }

    private void exportWeb(IRListing listing) {
        ComponentNameProvider<Web> vertexIdProvider = new IntegerComponentNameProvider<>();
        ComponentNameProvider<Web> vertexLabelProvider = new StringComponentNameProvider<>();
        GraphExporter<Web, DefaultEdge> exporter = new DOTExporter<>(vertexIdProvider, vertexLabelProvider, null, null, null);
        listing.getFunctions().forEach(function -> {
            Path path = file.resolveSibling("web." + function.getName() + ".dot");
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                exporter.exportGraph(function.getColoredGraph(), writer);
                System.out.println("web saved to " + path.toString());
            } catch (IOException | ExportException e) {
                e.printStackTrace();
            }
        });
        System.out.println();
    }

    private void exportCfg(IRListing listing) {
        ComponentNameProvider<BasicBlock> vertexIdProvider = new IntegerComponentNameProvider<>();
        ComponentNameProvider<BasicBlock> vertexLabelProvider = new StringComponentNameProvider<>();
        ComponentAttributeProvider<BasicBlock> vertexAttributeProvider = component -> Map.of("shape", new DefaultAttribute<>("Mrecord", AttributeType.STRING));
        GraphExporter<BasicBlock, DefaultEdge> exporter = new DOTExporter<>(vertexIdProvider, vertexLabelProvider, null, vertexAttributeProvider, null);

        listing.getFunctions().forEach(function -> {
            Path path = file.resolveSibling("cfg." + function.getName() + ".dot");
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                exporter.exportGraph(function.getCfg(), writer);
                System.out.println("cfg saved to " + path.toString());
            } catch (IOException | ExportException e) {
                e.printStackTrace();
            }
        });
        System.out.println();
    }

    private void printSemanticError(SemanticException e, CommonTokenStream stream) {
        Token token = stream.get(e.getToken().getSourceInterval().a);
        printError(token.getLine(), token.getCharPositionInLine(), e.getMessage());
    }

    private void printError(int line, int index, String message) {
        System.out.println("Error: line " + line + ":" + index + ": " + message);

        try (Stream<String> lines = Files.lines(file)) {
            lines.skip(line - 1).findFirst().ifPresent(s -> {
                System.out.println(s.stripTrailing());
                System.out.println(s.substring(0, index).replaceAll("\\S", " ") + "^");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class TigerDebugListener extends TigerParserBaseListener {
        List<TerminalNode> nodes = new ArrayList<>();

        @Override
        public void visitTerminal(TerminalNode node) {
            nodes.add(node);
        }
    }

    class TigerErrorListener extends BaseErrorListener {
        boolean hasError;

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            hasError = true;
            printError(line, charPositionInLine, msg);
        }
    }

    enum RegisterAllocationMode {
        BRIGGS, NAIVE
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
    }
}
