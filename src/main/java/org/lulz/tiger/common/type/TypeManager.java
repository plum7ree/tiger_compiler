package org.lulz.tiger.common.type;

public class TypeManager {
    private Type intType = new Type(TypeKind.INT);
    private Type floatType = new Type(TypeKind.FLOAT);

    public Type getIntPrimitiveType() {
        return intType;
    }

    public Type getFloatPrimitiveType() {
        return floatType;
    }

    public Type createArrayType(Type elementType, int size) {
        if (elementType.getKind() != TypeKind.INT && elementType.getKind() != TypeKind.FLOAT) {
            throw new RuntimeException("array element type must be int or float");
        }
        return new ArrayType(elementType, size);
    }

    public Type createFunctionType(Type[] args, Type retVal) {
        for (Type argType : args) {
            if (argType.getKind() == TypeKind.FUNCTION) {
                throw new RuntimeException("function parameter type cannot be function");
            }
        }
        if (retVal.getKind() != TypeKind.INT && retVal.getKind() != TypeKind.FLOAT) {
            throw new RuntimeException("return value type must be int or float");
        }
        return new FunctionType(args, retVal);
    }

    private boolean isPrimitive(Type type) {
        return type == intType || type == floatType;
    }

    public boolean isAssignableFrom(Type type1, Type type2) {
        if (type1.getKind() == TypeKind.FUNCTION || type1.getKind() == TypeKind.ARRAY
                || type2.getKind() == TypeKind.FUNCTION || type2.getKind() == TypeKind.ARRAY) {
            return false;
        }
        if (type1.getKind() == TypeKind.INT && type2.getKind() == TypeKind.FLOAT) { // no narrowing conversion
            return false;
        }
        return isPrimitive(type2) || type1 == type2; // primitives get promoted
    }

    public boolean areTypesCompatible(Type type1, Type type2) {
        return isAssignableFrom(type1, type2) || isAssignableFrom(type2, type1);
    }

    public Type getResultType(Type type1, Type type2) {
        if (!areTypesCompatible(type1, type2)) {
            throw new RuntimeException("incompatible types");
        }
        if (isAssignableFrom(type1, type2)) {
            return type1;
        } else if (isAssignableFrom(type2, type1)) {
            return type2;
        } else {
            throw new RuntimeException("sanity check failed"); // this should never happen
        }
    }
}
