package org.lulz.tiger.common.symbol;

import org.lulz.tiger.common.type.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolTable {
    private List<Scope> scopes = new ArrayList<>();
    private Deque<Scope> stack = new ArrayDeque<>();
    private Deque<Symbol> functionStack = new ArrayDeque<>();
    private int tempCounter = 0;
    private int constCounter = 0;
    private int labelCounter = 0;

    public int pushNewScope() {
        return pushNewScope("<anonymous scope>");
    }

    public int pushNewScope(String name) {
        if (!scopes.isEmpty() && stack.isEmpty()) {
            throw new RuntimeException("tried to push multiple root scopes");
        }

        int scopeId = scopes.size();
        Scope scope = new Scope(scopeId, name, stack.peek(), stack.size());
        scopes.add(scope);
        stack.push(scope);
        return scopeId;
    }

    public void pushScope(int scopeId) {
        if (scopeId >= scopes.size()) {
            throw new RuntimeException("tried to push invalid scopeId " + scopeId);
        }

        Scope scope = scopes.get(scopeId);
        if (scope.getParent() != stack.peek()) {
            throw new RuntimeException("scope pushed out of order");
        }

        stack.push(scopes.get(scopeId));
    }

    public void popScope() {
        stack.pop();
    }

    public void pushFunction(Symbol function) {
        if (function.getSymbolClass() != SymbolClass.FUNCTION) {
            throw new RuntimeException("not a function symbol");
        }
        functionStack.push(function);
    }

    public void popFunction() {
        functionStack.pop();
    }

    public Symbol addVariable(String name, Type type) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        Symbol symbol = new Symbol(name, SymbolClass.VAR, type);
        scope.put(name, symbol);

        Symbol function = functionStack.element();
        int index = function.getFrameSize();
        symbol.setFrameIndex(index);
        symbol.setFunction(function);
        function.setFrameSize(index + symbol.getMemorySize());

        return symbol;
    }

    public void addTypedef(String name, Type type) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        scope.put(name, new Symbol(name, SymbolClass.TYPEDEF, type));
    }

    public Symbol addFunction(String name, Type type) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        Symbol symbol = new Symbol(name, SymbolClass.FUNCTION, type);
        scope.put(name, symbol);
        return symbol;
    }

    public Symbol newTemporary(Type type) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        String name = "_t" + tempCounter;
        tempCounter++;
        Symbol symbol = new Symbol(name, SymbolClass.VAR, type);
        scope.put(name, symbol);

        Symbol function = functionStack.element();
        int index = function.getFrameSize();
        symbol.setFrameIndex(index);
        symbol.setFunction(function);
        function.setFrameSize(index + symbol.getMemorySize());

        return symbol;
    }

    public Symbol addConst(Type type, int val) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        String name = "_c" + constCounter;
        constCounter++;
        Symbol symbol = new Symbol(name, type, val);
        scope.put(name, symbol);
        return symbol;
    }

    public Symbol addConst(Type type, float val) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        String name = "_c" + constCounter;
        constCounter++;
        Symbol symbol = new Symbol(name, type, val);
        scope.put(name, symbol);
        return symbol;
    }

    public Symbol newLabel() {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        String name = "_label" + labelCounter;
        labelCounter++;
        Symbol symbol = new Symbol(name, SymbolClass.LABEL, null);
        scope.put(name, symbol);
        return symbol;
    }

    public Symbol getSymbol(String name) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }

        while (scope != null) {
            if (scope.contains(name)) {
                return scope.get(name);
            }
            scope = scope.getParent();
        }
        return null;
    }

    public boolean isDefinedInCurrentScope(String name) {
        Scope scope = stack.peek();
        if (scope == null) {
            throw new RuntimeException("no active scope");
        }
        return scope.contains(name);
    }

    @Override
    public String toString() {
        return scopes.stream().map(Scope::toString).collect(Collectors.joining("\n"));
    }
}
