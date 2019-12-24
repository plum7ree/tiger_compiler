package org.lulz.tiger.common;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.type.Type;

public class Decoration {
    private static ParseTreeProperty<Integer> scope = new ParseTreeProperty<>();
    private static ParseTreeProperty<Symbol> symbol = new ParseTreeProperty<>();
    private static ParseTreeProperty<Type> type = new ParseTreeProperty<>();
    private static ParseTreeProperty<Boolean> compar = new ParseTreeProperty<>();

    public static int getScopeId(ParseTree ctx) {
        return scope.get(ctx);
    }

    public static void putScopeId(ParseTree ctx, int scopeId) {
        scope.put(ctx, scopeId);
    }

    public static Symbol getSymbol(ParseTree ctx) {
        return symbol.get(ctx);
    }

    public static void putSymbol(ParseTree ctx, Symbol s) {
        symbol.put(ctx, s);
    }

    public static Type getType(ParseTree ctx) {
        return type.get(ctx);
    }

    public static void putType(ParseTree ctx, Type typeVal) {
        type.put(ctx, typeVal);
    }

    public static boolean isComparison(ParseTree ctx) {
        return compar.get(ctx);
    }

    public static void setComparison(ParseTree ctx, boolean b) {
        compar.put(ctx, b);
    }
}
