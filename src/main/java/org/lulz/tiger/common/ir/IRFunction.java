package org.lulz.tiger.common.ir;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.lulz.tiger.backend.liveness.Web;
import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.symbol.SymbolClass;
import org.lulz.tiger.common.type.FunctionType;
import org.lulz.tiger.common.type.Type;

import java.util.*;
import java.util.stream.Collectors;

public class IRFunction {
    private Symbol symbol;
    private List<IRInsn> instructions = new ArrayList<>();
    private List<BasicBlock> basicBlocks = new ArrayList<>();
    private Map<IRInsn, BasicBlock> leaderToBlock = new HashMap<>();
    private Graph<BasicBlock, DefaultEdge> cfg;

    // for Liveness Analyzer, DummyRegister Allocation
    private SimpleGraph<Web, DefaultEdge> coloredGraph;
    public void setColoredGraph(SimpleGraph<Web, DefaultEdge> g) { this.coloredGraph = g; }
    public SimpleGraph<Web, DefaultEdge> getColoredGraph() { return this.coloredGraph; }

    public IRFunction(Symbol symbol) {
        if (symbol.getSymbolClass() != SymbolClass.FUNCTION) {
            throw new RuntimeException("not a function");
        }
        this.symbol = symbol;
    }

    public String getName() {
        return symbol.getName();
    }

    public void emit(IRInsn insn) {
        insn.setFunction(this);
        instructions.add(insn);
    }

    public List<IRInsn> getInstructions() {
        return instructions;
    }

    public IRInsn getLabel(Symbol symbol) {
        if (symbol.getSymbolClass() != SymbolClass.LABEL) {
            throw new RuntimeException("not a label symbol");
        }
        return instructions.stream().filter(insn -> insn.isLabel() && insn.getLabel() == symbol)
                .findAny().orElseThrow(() -> new RuntimeException("label not found"));
    }

    public List<BasicBlock> getBasicBlocks() {
        return Collections.unmodifiableList(basicBlocks);
    }

    public void addBasicBlock(BasicBlock block) {
        basicBlocks.add(block);
        leaderToBlock.put(block.getLeader(), block);
    }

    public BasicBlock getBlockByLeader(IRInsn leader) {
        if (!leaderToBlock.containsKey(leader)) {
            throw new RuntimeException("no leader to block mapping");
        }
        return leaderToBlock.get(leader);
    }

    public Graph<BasicBlock, DefaultEdge> getCfg() {
        return cfg;
    }

    public void setCfg(Graph<BasicBlock, DefaultEdge> cfg) {
        this.cfg = cfg;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public int getFrameSize() {
        return symbol.getFrameSize();
    }

    @Override
    public String toString() {
        String params = Arrays.stream(((FunctionType) symbol.getType()).getArgs())
                .map(Type::toString).collect(Collectors.joining(", "));

        return "# start_function " + symbol.getName() + "\n" +
                symbol.getType().toString() + " " + symbol.getName() + "(" + params + "):\n" +
                instructions.stream().map(insn -> insn.isLabel() ? insn.toString() : "  " + insn.toString())
                        .collect(Collectors.joining("\n")) +
                "\n# end_function " + symbol.getName();
    }
}
