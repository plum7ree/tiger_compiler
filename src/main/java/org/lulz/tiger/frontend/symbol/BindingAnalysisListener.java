package org.lulz.tiger.frontend.symbol;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.lulz.tiger.antlr.TigerLexer;
import org.lulz.tiger.antlr.TigerParser;
import org.lulz.tiger.common.Decoration;
import org.lulz.tiger.common.symbol.ScopeBindingListener;
import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.symbol.SymbolTable;
import org.lulz.tiger.common.type.FunctionType;
import org.lulz.tiger.common.type.Type;
import org.lulz.tiger.common.type.TypeManager;
import org.lulz.tiger.frontend.SemanticException;

public class BindingAnalysisListener extends ScopeBindingListener {
    private SymbolTable symbols;
    private TypeManager typeManager;

    public BindingAnalysisListener(SymbolTable symbols, TypeManager typeManager) {
        super(symbols);
        this.symbols = symbols;
        this.typeManager = typeManager;
    }

    @Override
    public void enterTigerProgram(TigerParser.TigerProgramContext ctx) {
        super.enterTigerProgram(ctx);

        // define standard library functions
        symbols.addFunction("printi", new FunctionType(new Type[]{typeManager.getIntPrimitiveType()}, typeManager.getIntPrimitiveType()));
        symbols.addFunction("printf", new FunctionType(new Type[]{typeManager.getFloatPrimitiveType()}, typeManager.getIntPrimitiveType()));
        symbols.addFunction("flush", new FunctionType(new Type[0], typeManager.getIntPrimitiveType()));
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == TigerLexer.ID) {
            Symbol symbol = symbols.getSymbol(node.getText());
            if (symbol == null) {
                throw new SemanticException(node, "undeclared symbol");
            }
            Decoration.putSymbol(node, symbol);
            Decoration.putType(node, symbol.getType());
        } else if (node.getSymbol().getType() == TigerLexer.INTLIT) {
            try {
                int val = Integer.parseInt(node.getText());
                Symbol symbol = symbols.addConst(typeManager.getIntPrimitiveType(), val);
                Decoration.putSymbol(node, symbol);
                Decoration.putType(node, symbol.getType());
            } catch (NumberFormatException e) {
                throw new SemanticException(node, "invalid integer");
            }
        } else if (node.getSymbol().getType() == TigerLexer.FLOATLIT) {
            try {
                float val = Float.parseFloat(node.getText());
                Symbol symbol = symbols.addConst(typeManager.getFloatPrimitiveType(), val);
                Decoration.putSymbol(node, symbol);
                Decoration.putType(node, symbol.getType());
            } catch (NumberFormatException e) {
                throw new SemanticException(node, "invalid float");
            }
        }
    }
}
