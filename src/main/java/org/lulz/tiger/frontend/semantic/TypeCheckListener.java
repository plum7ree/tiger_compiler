package org.lulz.tiger.frontend.semantic;

import org.antlr.v4.runtime.ParserRuleContext;
import org.lulz.tiger.antlr.TigerParser;
import org.lulz.tiger.common.Decoration;
import org.lulz.tiger.common.symbol.ScopeBindingListener;
import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.symbol.SymbolClass;
import org.lulz.tiger.common.symbol.SymbolTable;
import org.lulz.tiger.common.type.*;
import org.lulz.tiger.frontend.SemanticException;

public class TypeCheckListener extends ScopeBindingListener {
    private TypeManager typeManager;

    public TypeCheckListener(SymbolTable symbols, TypeManager typeManager) {
        super(symbols);
        this.typeManager = typeManager;
    }

    @Override
    public void exitLiteral(TigerParser.LiteralContext ctx) {
        Decoration.putType(ctx, Decoration.getType(ctx.getChild(0)));
    }

    @Override
    public void exitLvalue(TigerParser.LvalueContext ctx) {
        Symbol symbol = Decoration.getSymbol(ctx.ID());
        if (symbol.getSymbolClass() != SymbolClass.VAR) {
            throw new SemanticException(ctx.ID(), "expected variable symbol");
        }
        Type type = Decoration.getType(ctx.ID());

        // is this an array access?
        if (ctx.LBRACK() != null) {
            // main type must be array
            if (type.getKind() != TypeKind.ARRAY) {
                throw new SemanticException(ctx.ID(), "expected array type");
            }
            // expression type must be int primitive (no derived types)
            Type exprType = Decoration.getType(ctx.expr());
            if (exprType != typeManager.getIntPrimitiveType()) {
                throw new SemanticException(ctx.expr(), "expected int type");
            }
            // lvalue type is element type
            Decoration.putType(ctx, ((ArrayType) type).getElementType());
        } else {
            // not array access
            Decoration.putType(ctx, type);
        }
    }

    @Override
    public void exitAtom(TigerParser.AtomContext ctx) {
        ParserRuleContext child;
        if (ctx.literal() != null) {
            child = ctx.literal();
            Decoration.setComparison(ctx, false);
        } else if (ctx.lvalue() != null) {
            child = ctx.lvalue();
            Decoration.setComparison(ctx, false);
        } else if (ctx.expr() != null) {
            child = ctx.expr();
            Decoration.setComparison(ctx, Decoration.isComparison(child));
        } else {
            throw new RuntimeException("atom without child");
        }
        Type type = Decoration.getType(child);
        Decoration.putType(ctx, type);
    }

    @Override
    public void exitExpr(TigerParser.ExprContext ctx) {
        if (ctx.atom() != null) { // atom
            Type type = Decoration.getType(ctx.atom());
            Decoration.putType(ctx, type);
            Decoration.setComparison(ctx, Decoration.isComparison(ctx.atom()));
        } else { // binary op
            Type type1 = Decoration.getType(ctx.expr(0));
            Type type2 = Decoration.getType(ctx.expr(1));
            Type resultType;
            switch (ctx.op.getType()) {
                case TigerParser.POW:
                    if (type1 != typeManager.getIntPrimitiveType() && type1 != typeManager.getFloatPrimitiveType()) {
                        throw new SemanticException(ctx.expr(0), "expected int or float");
                    }
                    if (type2 != typeManager.getIntPrimitiveType()) {
                        throw new SemanticException(ctx.expr(1), "expected int");
                    }
                    resultType = type1;
                    Decoration.setComparison(ctx, false);
                    break;
                case TigerParser.DIV:
                case TigerParser.MULT:
                case TigerParser.PLUS:
                case TigerParser.MINUS:
                    if (!typeManager.areTypesCompatible(type1, type2)) {
                        throw new SemanticException(ctx.getChild(1), "cannot operate on incompatible types");
                    }
                    resultType = typeManager.getResultType(type1, type2);
                    Decoration.setComparison(ctx, false);
                    break;
                case TigerParser.EQ:
                case TigerParser.NEQ:
                case TigerParser.GREATER:
                case TigerParser.LESSER:
                case TigerParser.GREATEREQ:
                case TigerParser.LESSEREQ:
                    if (!typeManager.areTypesCompatible(type1, type2)) {
                        throw new SemanticException(ctx.getChild(1), "cannot compare incompatible types");
                    }
                    if (Decoration.isComparison(ctx.expr(0))) {
                        throw new SemanticException(ctx.expr(0), "operand cannot be comparison");
                    }
                    if (Decoration.isComparison(ctx.expr(1))) {
                        throw new SemanticException(ctx.expr(1), "operand cannot be comparison");
                    }
                    resultType = typeManager.getIntPrimitiveType();
                    Decoration.setComparison(ctx, true);
                    break;
                case TigerParser.AND:
                case TigerParser.OR:
                    // valid operands are comparison results
                    /*if (!Decoration.isComparison(ctx.expr(0))) {
                        throw new SemanticException(ctx.expr(0), "operand must be comparison");
                    }
                    if (!Decoration.isComparison(ctx.expr(1))) {
                        throw new SemanticException(ctx.expr(1), "operand must be comparison");
                    }*/
                    resultType = typeManager.getIntPrimitiveType();
                    Decoration.setComparison(ctx, false);
                    break;
                default:
                    throw new RuntimeException("unknown op"); // shouldn't happen
            }
            Decoration.putType(ctx, resultType);
        }
    }

    @Override
    public void exitAssignment(TigerParser.AssignmentContext ctx) {
        Type ltype = Decoration.getType(ctx.lvalue());
        Type rtype = Decoration.getType(ctx.expr());
        if (!typeManager.isAssignableFrom(ltype, rtype)) {
            throw new SemanticException(ctx.ASSIGN(), "cannot assign: incompatible types");
        }
    }

    @Override
    public void exitOptionalInit(TigerParser.OptionalInitContext ctx) {
        Type ltype = Decoration.getType(ctx.parent);
        Type rtype = Decoration.getType(ctx.literal());
        if (ltype.getKind() == TypeKind.ARRAY) {
            ltype = ((ArrayType) ltype).getElementType();
        }
        if (!typeManager.isAssignableFrom(ltype, rtype)) {
            throw new SemanticException(ctx.ASSIGN(), "cannot assign: incompatible types");
        }
    }

    @Override
    public void exitFncallAssignment(TigerParser.FncallAssignmentContext ctx) {
        Symbol symbol = Decoration.getSymbol(ctx.ID());
        if (symbol.getSymbolClass() != SymbolClass.VAR) {
            throw new SemanticException(ctx.ID(), "expected variable symbol");
        }
        Type ltype = Decoration.getType(ctx.ID());
        Type rtype = Decoration.getType(ctx.fncall());
        if (!typeManager.isAssignableFrom(ltype, rtype)) {
            throw new SemanticException(ctx.ASSIGN(), "cannot assign: incompatible types");
        }
    }

    @Override
    public void exitFncall(TigerParser.FncallContext ctx) {
        Type fntype = Decoration.getType(ctx.ID());
        if (fntype.getKind() != TypeKind.FUNCTION) {
            throw new SemanticException(ctx.ID(), "expected function identifier");
        }

        // check function args
        Type[] argTypes = ((FunctionType) fntype).getArgs();
        int nargs = ctx.expressionList() == null ? 0 : ctx.expressionList().expr().size();
        if (argTypes.length != nargs) {
            throw new SemanticException(ctx.expressionList(), "expected " + argTypes.length + " arguments");
        }
        for (int i = 0; i < argTypes.length; i++) {
            Type givenType = Decoration.getType(ctx.expressionList().expr(i));
            if (!typeManager.isAssignableFrom(argTypes[0], givenType)
                    && argTypes[0] != givenType) { // isAssignableFrom doesn't handle array type
                throw new SemanticException(ctx.expressionList().expr(i), "incompatible argument type");
            }
        }

        // set return type
        Decoration.putType(ctx, ((FunctionType) fntype).getRetVal());
    }

    @Override
    public void exitIfBlock(TigerParser.IfBlockContext ctx) {
        Type type = Decoration.getType(ctx.expr());
        if (type != typeManager.getIntPrimitiveType()) {
            throw new SemanticException(ctx.expr(), "expected int type");
        }
    }

    @Override
    public void exitWhileBlock(TigerParser.WhileBlockContext ctx) {
        Type type = Decoration.getType(ctx.expr());
        if (type != typeManager.getIntPrimitiveType()) {
            throw new SemanticException(ctx.expr(), "expected int type");
        }
    }

    @Override
    public void exitForBlock(TigerParser.ForBlockContext ctx) {
        Type varType = Decoration.getType(ctx.ID());
        if (varType != typeManager.getIntPrimitiveType()) {
            throw new SemanticException(ctx.ID(), "expected int type");
        }
        Type expr1Type = Decoration.getType(ctx.expr(0));
        if (expr1Type != typeManager.getIntPrimitiveType()) {
            throw new SemanticException(ctx.expr(0), "expected int type");
        }
        Type expr2Type = Decoration.getType(ctx.expr(1));
        if (expr2Type != typeManager.getIntPrimitiveType()) {
            throw new SemanticException(ctx.expr(1), "expected int type");
        }
    }
}
