package org.lulz.tiger.common.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Scope {
    private int scopeId;
    private String name;
    private Scope parent;
    private int depth;
    private Map<String, Symbol> map = new HashMap<>();

    Scope(int scopeId, String name, Scope parent, int depth) {
        this.scopeId = scopeId;
        this.name = name;
        this.parent = parent;
        this.depth = depth;
    }

    public int getScopeId() {
        return scopeId;
    }

    public String getName() {
        return name;
    }

    Scope getParent() {
        return parent;
    }

    void put(String name, Symbol symbol) {
        if (map.containsKey(name)) {
            throw new RuntimeException("symbol already defined in scope");
        }
        map.put(name, symbol);
    }

    Symbol get(String name) {
        return map.get(name);
    }

    boolean contains(String name) {
        return map.containsKey(name);
    }

    @Override
    public String toString() {
        return " ".repeat(depth * 2) + "scope " + scopeId + ":\n"
                + map.values().stream()
                .map(symbol -> symbol.toString() + ", " + symbol.getSymbolClass() + ", " + symbol.getType())
                .map(s -> " ".repeat(depth * 2 + 2) + s)
                .collect(Collectors.joining("\n"));
    }
}
