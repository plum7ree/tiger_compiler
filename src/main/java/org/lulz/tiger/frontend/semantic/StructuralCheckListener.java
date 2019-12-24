package org.lulz.tiger.frontend.semantic;

import org.antlr.v4.runtime.tree.ParseTree;
import org.lulz.tiger.antlr.TigerParser;
import org.lulz.tiger.common.Decoration;
import org.lulz.tiger.common.symbol.ScopeBindingListener;
import org.lulz.tiger.common.symbol.SymbolTable;
import org.lulz.tiger.common.type.FunctionType;
import org.lulz.tiger.common.type.Type;
import org.lulz.tiger.common.type.TypeManager;
import org.lulz.tiger.frontend.SemanticException;

import java.util.ArrayDeque;
import java.util.Deque;

public class StructuralCheckListener extends ScopeBindingListener {
    private TypeManager typeManager;
    private Deque<FunctionMeta> functionStack = new ArrayDeque<>();
    private Deque<ControlMeta> controlStack = new ArrayDeque<>(); // for loop constructs

    public StructuralCheckListener(SymbolTable symbols, TypeManager typeManager) {
        super(symbols);
        this.typeManager = typeManager;
    }

    @Override
    public void enterFunctionDeclaration(TigerParser.FunctionDeclarationContext ctx) {
        super.enterFunctionDeclaration(ctx);
        if (!functionStack.isEmpty()) {
            throw new SemanticException(ctx, "nested functions are not allowed");
        }

        functionStack.push(new FunctionMeta(Decoration.getType(ctx)));
        controlStack.push(new ControlMeta(null, false));
    }

    @Override
    public void exitFunctionDeclaration(TigerParser.FunctionDeclarationContext ctx) {
        super.exitFunctionDeclaration(ctx);
        if (!functionStack.pop().hasReturn) {
            throw new SemanticException(ctx.END(), "missing return statement");
        }

        controlStack.pop();
    }

    @Override
    public void exitReturn(TigerParser.ReturnContext ctx) {
        if (functionStack.isEmpty()) {
            throw new SemanticException(ctx, "return used outside function");
        }
        functionStack.peek().hasReturn = true;

        Type returnType = ((FunctionType) functionStack.peek().functionType).getRetVal();
        if (!typeManager.isAssignableFrom(returnType, Decoration.getType(ctx.expr()))) {
            throw new SemanticException(ctx.expr(), "incompatible return type");
        }
    }

    @Override
    public void enterWhileBlock(TigerParser.WhileBlockContext ctx) {
        controlStack.push(new ControlMeta(ctx, true));
    }

    @Override
    public void exitWhileBlock(TigerParser.WhileBlockContext ctx) {
        controlStack.pop();
    }

    @Override
    public void enterForBlock(TigerParser.ForBlockContext ctx) {
        controlStack.push(new ControlMeta(ctx, true));
    }

    @Override
    public void exitForBlock(TigerParser.ForBlockContext ctx) {
        controlStack.pop();
    }

    @Override
    public void exitBreak(TigerParser.BreakContext ctx) {
        if (controlStack.isEmpty() || !controlStack.peek().isInLoop) {
            throw new SemanticException(ctx, "break used outside loop");
        }
    }

    private static class FunctionMeta {
        private FunctionMeta(Type functionType) {
            this.functionType = functionType;
        }

        Type functionType;
        boolean hasReturn;
    }

    private static class ControlMeta {
        private ControlMeta(ParseTree node, boolean isInLoop) {
            this.node = node;
            this.isInLoop = isInLoop;
        }

        ParseTree node;
        boolean isInLoop;
    }
}
