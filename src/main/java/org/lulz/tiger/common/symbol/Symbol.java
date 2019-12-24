package org.lulz.tiger.common.symbol;

import org.lulz.tiger.common.ir.IRFunction;
import org.lulz.tiger.common.type.ArrayType;
import org.lulz.tiger.common.type.Type;
import org.lulz.tiger.common.type.TypeKind;

public class Symbol {
    private String name;
    private SymbolClass symbolClass;
    private Type type;
    private int intVal;
    private float floatVal;
    private boolean isArgument;
    private int frameIndex;
    private int frameSize;
    private Symbol function;
    private IRFunction irFunction;

    public Symbol(String name, SymbolClass symbolClass, Type type) {
        this.name = name;
        this.symbolClass = symbolClass;
        this.type = type;
    }

    public Symbol(String name, Type type, int val) {
        this(name, SymbolClass.ICONST, type);
        this.intVal = val;
    }

    public Symbol(String name, Type type, float val) {
        this(name, SymbolClass.FCONST, type);
        this.floatVal = val;
    }

    public int getIntVal() {
        return intVal;
    }

    public float getFloatVal() {
        return floatVal;
    }

    public boolean isArgument() {
        return isArgument;
    }

    public void setArgument(boolean argument) {
        isArgument = argument;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public int getMemorySize() {
        if (type.getKind() == TypeKind.ARRAY) {
            return ((ArrayType) type).getSize() * 4;
        }
        return 4;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;
    }

    public Symbol getFunction() {
        return function;
    }

    public void setFunction(Symbol function) {
        this.function = function;
    }

    public IRFunction getIrFunction() {
        return irFunction;
    }

    public void setIrFunction(IRFunction irFunction) {
        this.irFunction = irFunction;
    }

    public String getName() {
        return name;
    }

    public SymbolClass getSymbolClass() {
        return symbolClass;
    }

    public Type getType() {
        return type;
    }

    public boolean isConstant() {
        return symbolClass == SymbolClass.ICONST || symbolClass == SymbolClass.FCONST;
    }

    @Override
    public String toString() {
        return name;
    }
}
