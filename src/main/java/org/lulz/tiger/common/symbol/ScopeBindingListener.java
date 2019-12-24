package org.lulz.tiger.common.symbol;

import org.lulz.tiger.antlr.TigerParser;
import org.lulz.tiger.antlr.TigerParserBaseListener;
import org.lulz.tiger.common.Decoration;

public abstract class ScopeBindingListener extends TigerParserBaseListener {
    private SymbolTable symbols;

    public ScopeBindingListener(SymbolTable symbols) {
        this.symbols = symbols;
    }

    @Override
    public void enterTigerProgram(TigerParser.TigerProgramContext ctx) {
        symbols.pushScope(Decoration.getScopeId(ctx));
    }

    @Override
    public void exitTigerProgram(TigerParser.TigerProgramContext ctx) {
        symbols.popScope();
    }

    @Override
    public void enterLetBlock(TigerParser.LetBlockContext ctx) {
        symbols.pushScope(Decoration.getScopeId(ctx));
    }

    @Override
    public void exitLetBlock(TigerParser.LetBlockContext ctx) {
        symbols.popScope();
    }

    @Override
    public void enterFunctionDeclaration(TigerParser.FunctionDeclarationContext ctx) {
        symbols.pushScope(Decoration.getScopeId(ctx));
    }

    @Override
    public void exitFunctionDeclaration(TigerParser.FunctionDeclarationContext ctx) {
        symbols.popScope();
    }
}
