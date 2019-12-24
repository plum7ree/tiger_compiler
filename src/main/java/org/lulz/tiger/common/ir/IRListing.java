package org.lulz.tiger.common.ir;

import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.symbol.SymbolClass;

import java.util.*;
import java.util.stream.Collectors;

public class IRListing {
    private List<IRFunction> functions = new ArrayList<>();
    private Deque<IRFunction> stack = new ArrayDeque<>();

    public void pushFunction(Symbol symbol) {
        IRFunction func = new IRFunction(symbol);
        symbol.setIrFunction(func);
        stack.push(func);
        functions.add(func);
    }

    public void popFunction() {
        stack.pop();
    }

    public void emit(Opcode op, Symbol... operands) {
        if (Arrays.stream(operands).anyMatch(Objects::isNull)) {
            throw new RuntimeException("null operand");
        }
        stack.peek().emit(new IRInsn(op, operands));
    }

    public void emit(Symbol label) {
        if (label.getSymbolClass() != SymbolClass.LABEL) {
            throw new RuntimeException("not a label");
        }
        stack.peek().emit(new IRInsn(label));
    }

    public List<IRFunction> getFunctions() {
        return functions;
    }

    @Override
    public String toString() {
        return functions.stream().map(IRFunction::toString).collect(Collectors.joining("\n\n"));
    }
}
