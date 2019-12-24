package org.lulz.tiger.frontend.irgen;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.lulz.tiger.antlr.TigerParser;
import org.lulz.tiger.antlr.TigerParserBaseVisitor;
import org.lulz.tiger.common.Decoration;
import org.lulz.tiger.common.ir.IRListing;
import org.lulz.tiger.common.ir.Opcode;
import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.symbol.SymbolTable;
import org.lulz.tiger.common.type.TypeKind;
import org.lulz.tiger.common.type.TypeManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import static org.lulz.tiger.common.ir.Opcode.*;

public class IRGenVisitor extends TigerParserBaseVisitor<Symbol> {
    private SymbolTable symbols;
    private TypeManager typeManager;
    private IRListing listing = new IRListing();
    private Deque<Symbol> breakTargets = new ArrayDeque<>();

    public IRGenVisitor(SymbolTable symbols, TypeManager typeManager) {
        this.symbols = symbols;
        this.typeManager = typeManager;
    }

    public IRListing getListing() {
        return listing;
    }

    @Override
    public Symbol visitTigerProgram(TigerParser.TigerProgramContext ctx) {
        symbols.pushScope(Decoration.getScopeId(ctx));
        symbols.pushFunction(Decoration.getSymbol(ctx));
        listing.pushFunction(Decoration.getSymbol(ctx));

        visitChildren(ctx);
        listing.emit(RETURN, iconst(0));

        symbols.popScope();
        symbols.popFunction();
        listing.popFunction();
        return null;
    }

    @Override
    public Symbol visitFunctionDeclaration(TigerParser.FunctionDeclarationContext ctx) {
        symbols.pushScope(Decoration.getScopeId(ctx));
        symbols.pushFunction(Decoration.getSymbol(ctx));
        listing.pushFunction(Decoration.getSymbol(ctx));
        visitChildren(ctx);
        symbols.popScope();
        symbols.popFunction();
        listing.popFunction();
        return null;
    }

    @Override
    public Symbol visitLetBlock(TigerParser.LetBlockContext ctx) {
        symbols.pushScope(Decoration.getScopeId(ctx));
        visitChildren(ctx);
        symbols.popScope();
        return null;
    }

    @Override
    public Symbol visitAssignment(TigerParser.AssignmentContext ctx) {
        Symbol rvalue = visitExpr(ctx.expr());
        if (ctx.lvalue().LBRACK() != null) { // array
            Symbol index = visitExpr(ctx.lvalue().expr());
            Symbol array = visitTerminal(ctx.lvalue().ID());
            listing.emit(STORE, rvalue, array, index);
        } else { // scalar
            listing.emit(ASSIGN, visitTerminal(ctx.lvalue().ID()), rvalue);
        }
        return null;
    }

    @Override
    public Symbol visitFncallAssignment(TigerParser.FncallAssignmentContext ctx) {
        Symbol lvalue = visitTerminal(ctx.ID());
        Symbol rvalue = visitFncall(ctx.fncall());
        listing.emit(ASSIGN, lvalue, rvalue);
        return null;
    }

    @Override
    public Symbol visitVarDeclaration(TigerParser.VarDeclarationContext ctx) {
        if (ctx.optionalInit() != null) {
            for (TerminalNode id : ctx.idList().ID()) {
                Symbol lvalue = visitTerminal(id);
                Symbol rvalue = visitLiteral(ctx.optionalInit().literal());
                if (lvalue.getType().getKind() == TypeKind.ARRAY) {
                    listing.emit(ARRINIT, lvalue, rvalue);
                } else {
                    listing.emit(ASSIGN, lvalue, rvalue);
                }
            }
        }
        return null;
    }

    @Override
    public Symbol visitExpr(TigerParser.ExprContext ctx) {
        if (ctx.atom() != null) {
            return visitChildren(ctx);
        }

        Symbol o1 = visitExpr(ctx.expr(0));
        Symbol o2 = visitExpr(ctx.expr(1));
        Symbol result = symbols.newTemporary(Decoration.getType(ctx));

        switch (ctx.op.getType()) {
            case TigerParser.POW: // desugar POW into multiplication loop
                Symbol repeatLabel = symbols.newLabel();
                Symbol counter = symbols.newTemporary(typeManager.getIntPrimitiveType());

                listing.emit(ASSIGN, counter, iconst(1));
                listing.emit(ASSIGN, result, o1);
                listing.emit(repeatLabel);
                listing.emit(MUL, result, result, o1);
                listing.emit(ADD, counter, counter, iconst(1));
                listing.emit(BLT, counter, o2, repeatLabel);
                break;

            case TigerParser.DIV:
            case TigerParser.MULT:
            case TigerParser.PLUS:
            case TigerParser.MINUS:
            case TigerParser.AND:
            case TigerParser.OR:
                listing.emit(getExprOpcode(ctx.op.getType()), result, o1, o2);
                break;

            case TigerParser.EQ:
            case TigerParser.NEQ:
            case TigerParser.GREATER:
            case TigerParser.LESSER:
            case TigerParser.GREATEREQ:
            case TigerParser.LESSEREQ:
                Symbol trueLabel = symbols.newLabel();
                Symbol endLabel = symbols.newLabel();
                listing.emit(getExprOpcode(ctx.op.getType()), o1, o2, trueLabel);
                listing.emit(ASSIGN, result, iconst(0));
                listing.emit(GOTO, endLabel);

                listing.emit(trueLabel);
                listing.emit(ASSIGN, result, iconst(1));
                listing.emit(endLabel);
                break;
            default:
                throw new RuntimeException("unknown op");
        }
        return result;
    }

    @Override
    public Symbol visitFncall(TigerParser.FncallContext ctx) {
        Symbol result = symbols.newTemporary(Decoration.getType(ctx));
        List<Symbol> operands;
        if (ctx.expressionList() != null) {
            operands = ctx.expressionList().expr().stream().map(this::visitExpr).collect(Collectors.toList());
        } else {
            operands = new ArrayList<>();
        }
        operands.add(0, result);
        operands.add(1, visitTerminal(ctx.ID()));
        listing.emit(CALL, operands.toArray(new Symbol[0]));
        return result;
    }

    @Override
    public Symbol visitReturn(TigerParser.ReturnContext ctx) {
        Symbol value = visitExpr(ctx.expr());
        listing.emit(RETURN, value);
        return null;
    }

    private Opcode getExprOpcode(int op) {
        switch (op) {
            /*case TigerParser.POW: // we desugar exponentiation at the IR level
                return POW;*/
            case TigerParser.DIV:
                return DIV;
            case TigerParser.MULT:
                return MUL;
            case TigerParser.PLUS:
                return ADD;
            case TigerParser.MINUS:
                return SUB;

            case TigerParser.EQ:
                return BEQ;
            case TigerParser.NEQ:
                return BNE;
            case TigerParser.GREATER:
                return BGT;
            case TigerParser.LESSER:
                return BLT;
            case TigerParser.GREATEREQ:
                return BGE;
            case TigerParser.LESSEREQ:
                return BLE;

            case TigerParser.AND:
                return AND;
            case TigerParser.OR:
                return OR;
        }
        throw new RuntimeException("invalid op");
    }

    @Override
    public Symbol visitLvalue(TigerParser.LvalueContext ctx) { // actually an rvalue :^)
        if (ctx.LBRACK() != null) { // array
            Symbol result = symbols.newTemporary(Decoration.getType(ctx));
            Symbol index = visitExpr(ctx.expr());
            Symbol array = visitTerminal(ctx.ID());
            listing.emit(LOAD, result, array, index);
            return result;
        } else { // scalar
            return visitTerminal(ctx.ID());
        }
    }

    private void branchOnExpr(TigerParser.ExprContext ctx, Symbol trueLabel) {
        // strip redundant parenthesized expressions
        TigerParser.AtomContext atom = ctx.atom();
        while (atom != null) {
            ctx = atom.expr();
            atom = ctx.atom();
        }

        if (Decoration.isComparison(ctx)) {
            Symbol o1 = visitExpr(ctx.expr(0));
            Symbol o2 = visitExpr(ctx.expr(1));
            Opcode opcode = getExprOpcode(ctx.op.getType());
            listing.emit(opcode, o1, o2, trueLabel);
        } else { // do comparison ourselves. 0 is falsy.
            Symbol result = visitExpr(ctx);
            listing.emit(BNE, result, iconst(0), trueLabel);
        }
    }

    @Override
    public Symbol visitIfBlock(TigerParser.IfBlockContext ctx) {
        Symbol trueLabel = symbols.newLabel();
        Symbol endLabel = symbols.newLabel();
        branchOnExpr(ctx.expr(), trueLabel);
        if (ctx.ELSE() != null) { // do we have an else block
            visitStatSeq(ctx.statSeq(1));
        }
        listing.emit(GOTO, endLabel);

        listing.emit(trueLabel);
        visitStatSeq(ctx.statSeq(0));
        listing.emit(endLabel);
        return null;
    }

    @Override
    public Symbol visitWhileBlock(TigerParser.WhileBlockContext ctx) {
        Symbol startLabel = symbols.newLabel();
        Symbol trueLabel = symbols.newLabel();
        Symbol endLabel = symbols.newLabel();

        listing.emit(startLabel);
        branchOnExpr(ctx.expr(), trueLabel);
        listing.emit(GOTO, endLabel);

        listing.emit(trueLabel);
        breakTargets.push(endLabel);    // push break target
        visitStatSeq(ctx.statSeq());
        breakTargets.pop();             // pop break target
        listing.emit(GOTO, startLabel);

        listing.emit(endLabel);
        return null;
    }

    @Override
    public Symbol visitForBlock(TigerParser.ForBlockContext ctx) {
        Symbol startLabel = symbols.newLabel();
        Symbol trueLabel = symbols.newLabel();
        Symbol endLabel = symbols.newLabel();

        Symbol counter = symbols.newTemporary(typeManager.getIntPrimitiveType());
        Symbol initial = visitExpr(ctx.expr(0));
        listing.emit(ASSIGN, counter, initial);

        listing.emit(startLabel);
        listing.emit(ASSIGN, visitTerminal(ctx.ID()), counter); // bind var to counter
        Symbol bound = visitExpr(ctx.expr(1));
        listing.emit(BLE, counter, bound, trueLabel);
        listing.emit(GOTO, endLabel);

        listing.emit(trueLabel);
        breakTargets.push(endLabel);    // push break target
        visitStatSeq(ctx.statSeq());
        breakTargets.pop();             // pop break target

        listing.emit(ADD, counter, counter, iconst(1));
        listing.emit(GOTO, startLabel);

        listing.emit(endLabel);
        return null;
    }

    @Override
    public Symbol visitBreak(TigerParser.BreakContext ctx) {
        if (breakTargets.isEmpty()) {
            throw new RuntimeException("no break target"); // should be prevented by semantic check
        }
        listing.emit(GOTO, breakTargets.peek());
        return null;
    }

    @Override
    public Symbol visitTerminal(TerminalNode node) {
        return Decoration.getSymbol(node);
    }

    private Symbol iconst(int val) {
        return symbols.addConst(typeManager.getIntPrimitiveType(), val);
    }
}
