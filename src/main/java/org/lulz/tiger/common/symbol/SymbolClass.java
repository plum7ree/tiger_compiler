package org.lulz.tiger.common.symbol;

public enum SymbolClass {
    VAR("var"), TYPEDEF("type"), FUNCTION("func"), ICONST("iconst"), FCONST("fconst"), LABEL("label");

    private String name;
    SymbolClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
