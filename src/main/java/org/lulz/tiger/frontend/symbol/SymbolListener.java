package org.lulz.tiger.frontend.symbol;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.lulz.tiger.antlr.TigerParser;
import org.lulz.tiger.antlr.TigerParserBaseListener;
import org.lulz.tiger.common.Decoration;
import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.symbol.SymbolClass;
import org.lulz.tiger.common.symbol.SymbolTable;
import org.lulz.tiger.common.type.FunctionType;
import org.lulz.tiger.common.type.Type;
import org.lulz.tiger.common.type.TypeKind;
import org.lulz.tiger.common.type.TypeManager;
import org.lulz.tiger.frontend.SemanticException;

import java.util.ArrayList;
import java.util.List;

public class SymbolListener extends TigerParserBaseListener {
    private SymbolTable symbols;
    private TypeManager typeManager;

    public SymbolListener(SymbolTable symbols, TypeManager typeManager) {
        this.symbols = symbols;
        this.typeManager = typeManager;
    }

    @Override
    public void enterTigerProgram(TigerParser.TigerProgramContext ctx) {
        int scopeId = symbols.pushNewScope("<global>");
        Decoration.putScopeId(ctx, scopeId);

        // define main function
        Symbol main = symbols.addFunction("main", new FunctionType(new Type[0], typeManager.getIntPrimitiveType()));
        symbols.pushFunction(main);
        Decoration.putSymbol(ctx, main);
    }

    @Override
    public void exitTigerProgram(TigerParser.TigerProgramContext ctx) {
        symbols.popFunction();
        symbols.popScope();
    }

    @Override
    public void enterLetBlock(TigerParser.LetBlockContext ctx) {
        int scopeId = symbols.pushNewScope();
        Decoration.putScopeId(ctx, scopeId);
    }

    @Override
    public void exitLetBlock(TigerParser.LetBlockContext ctx) {
        symbols.popScope();
    }

    @Override
    public void enterFunctionDeclaration(TigerParser.FunctionDeclarationContext ctx) {
        // register function symbol
        List<Type> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        if (ctx.paramList() != null) {
            for (TigerParser.ParamContext pctx : ctx.paramList().param()) {
                Type paramType = resolveType(pctx.type());
                if (paramType.getKind() == TypeKind.ARRAY) {
                    throw new SemanticException(pctx.type(), "function parameter cannot be array");
                }
                paramTypes.add(paramType);
                paramNames.add(pctx.ID().getText());
                if (paramTypes.size() > 4) {
                    throw new SemanticException(pctx, "function has more than four parameters");
                }
            }
        }

        Type retVal = resolveType(ctx.type());
        if (retVal.getKind() == TypeKind.ARRAY) {
            throw new SemanticException(ctx.type(), "function cannot return array type");
        }

        Type funcType = typeManager.createFunctionType(paramTypes.toArray(new Type[0]), retVal);
        if (symbols.isDefinedInCurrentScope(ctx.ID().getText())) {
            throw new SemanticException(ctx.ID(), "symbol already defined in scope");
        }

        Symbol funcSymbol = symbols.addFunction(ctx.ID().getText(), funcType);
        symbols.pushFunction(funcSymbol);
        Decoration.putType(ctx, funcType);
        Decoration.putSymbol(ctx, funcSymbol);

        // push function scope
        int scopeId = symbols.pushNewScope(ctx.ID().getText());
        Decoration.putScopeId(ctx, scopeId);

        // register function argument variables
        for (int i = 0; i < paramTypes.size(); i++) {
            if (symbols.isDefinedInCurrentScope(paramNames.get(i))) {
                throw new SemanticException(ctx.paramList().param(i), "symbol already defined in scope");
            }
            symbols.addVariable(paramNames.get(i), paramTypes.get(i)).setArgument(true);
        }
    }

    @Override
    public void exitFunctionDeclaration(TigerParser.FunctionDeclarationContext ctx) {
        symbols.popFunction();
        symbols.popScope();
    }

    @Override
    public void enterTypeDeclaration(TigerParser.TypeDeclarationContext ctx) {
        Type type = createType(ctx.type());
        if (symbols.isDefinedInCurrentScope(ctx.ID().getText())) {
            throw new SemanticException(ctx.ID(), "symbol already defined in scope");
        }
        symbols.addTypedef(ctx.ID().getText(), type);
    }

    @Override
    public void enterVarDeclaration(TigerParser.VarDeclarationContext ctx) {
        Type type = resolveType(ctx.type());
        for (TerminalNode id : ctx.idList().ID()) {
            if (symbols.isDefinedInCurrentScope(id.getText())) {
                throw new SemanticException(id, "symbol already defined in scope");
            }
            symbols.addVariable(id.getText(), type);
        }
        Decoration.putType(ctx, type); // for optionalInit type check
    }

    private Type createType(TigerParser.TypeContext ctx) {
        if (ctx.ARRAY() != null) {
            Type elementType = resolvePrimitiveType(ctx.typeId());
            int size = Integer.parseInt(ctx.INTLIT().getText());
            return typeManager.createArrayType(elementType, size);
        } else {
            return resolveType(ctx).clone();
        }
    }

    private Type resolveType(TigerParser.TypeContext ctx) {
        if (ctx.ARRAY() != null) {
            throw new SemanticException(ctx, "arrays must use named types");
        } else if (ctx.typeId() != null) {
            return resolvePrimitiveType(ctx.typeId());
        } else if (ctx.ID() != null) {
            Symbol symbol = symbols.getSymbol(ctx.ID().getText());
            if (symbol == null) {
                throw new SemanticException(ctx.ID(), "undeclared type");
            }
            if (symbol.getSymbolClass() != SymbolClass.TYPEDEF) {
                throw new SemanticException(ctx.ID(), "expected type symbol");
            }
            return symbol.getType();
        }
        throw new RuntimeException("failed to resolve type"); // sanity check
    }

    private Type resolvePrimitiveType(TigerParser.TypeIdContext ctx) {
        if (ctx.INT() != null) {
            return typeManager.getIntPrimitiveType();
        } else if (ctx.FLOAT() != null) {
            return typeManager.getFloatPrimitiveType();
        }
        throw new RuntimeException("failed to resolve primitive type"); // sanity check
    }
}
