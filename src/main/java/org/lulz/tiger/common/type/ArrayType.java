package org.lulz.tiger.common.type;

public class ArrayType extends Type {
    private Type elementType;
    private int size;

    public ArrayType(Type elementType, int size) {
        super(TypeKind.ARRAY);
        this.elementType = elementType;
        this.size = size;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return elementType.toString() + "[" + size + "]";
    }
}
