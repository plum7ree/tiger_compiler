package org.lulz.tiger.common.type;

public class FunctionType extends Type {
    private Type[] args;
    private Type retVal;

    public FunctionType(Type[] args, Type retVal) {
        super(TypeKind.FUNCTION);
        this.args = args;
        this.retVal = retVal;
    }

    public Type[] getArgs() {
        return args;
    }

    public Type getRetVal() {
        return retVal;
    }

    @Override
    public String toString() {
        return retVal.toString();
    }
}
