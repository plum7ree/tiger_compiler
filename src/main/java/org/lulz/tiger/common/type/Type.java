package org.lulz.tiger.common.type;

public class Type implements Cloneable {
    private TypeKind kind;

    public Type(TypeKind kind) {
        this.kind = kind;
    }

    public TypeKind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        if (kind == TypeKind.INT) {
            return "int";
        } else if (kind == TypeKind.FLOAT) {
            return "float";
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Type clone() {
        try {
            return (Type) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
