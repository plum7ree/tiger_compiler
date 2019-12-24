package org.lulz.tiger.common.ir;

import org.lulz.tiger.common.symbol.Symbol;
import org.lulz.tiger.common.symbol.SymbolClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lulz.tiger.common.ir.Opcode.GOTO;
import static org.lulz.tiger.common.ir.Opcode.RETURN;

public class IRInsn {
    private Opcode opcode;
    private Symbol[] operands;
    private Symbol label;
    private IRFunction function;
    private boolean isLabel;
    private boolean isLeader;
    private Set<Symbol> inSet;
    private Set<Symbol> outSet;

    public IRInsn(Opcode op, Symbol... operands) {
        this.opcode = op;
        this.operands = operands;
    }

    public IRInsn(Symbol label) {
        this.label = label;
        this.isLabel = true;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public Symbol[] getOperands() {
        return operands;
    }

    public Symbol getLabel() {
        return label;
    }

    public boolean isLabel() {
        return isLabel;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        isLeader = leader;
    }

    public IRFunction getFunction() {
        return function;
    }

    public void setFunction(IRFunction function) {
        this.function = function;
    }

    public boolean isFallThrough() {
        return opcode != GOTO && opcode != RETURN;
    }

    public IRInsn getBranchTarget() {
        if (isLabel) {
            return null;
        }
        switch (opcode) {
            case GOTO:
                return function.getLabel(operands[0]);
            case BEQ:
            case BNE:
            case BLT:
            case BGT:
            case BGE:
            case BLE:
                return function.getLabel(operands[2]);
            default:
                return null;
        }
    }

    public Set<Symbol> getDefs() {
        if (isLabel) {
            return Collections.emptySet();
        }
        switch (getOpcode()) {
            // these instructions perform an assignment
            case ASSIGN:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case AND:
            case OR:
            case CALL:
            case LOAD:
                return Collections.singleton(operands[0]);
            default:
                return Collections.emptySet();
        }
    }

    public Set<Symbol> getUses() {
        if (isLabel) {
            return Collections.emptySet();
        }
        Set<Symbol> uses = new HashSet<>();
        switch (getOpcode()) {
            case ASSIGN:
            case ARRINIT:    // IR generator will never emit an ARINIT with register uses, but put this here anyway
                uses.add(operands[1]);
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case AND:
            case OR:
                uses.add(operands[1]);
                uses.add(operands[2]);
                break;
            case STORE:
                uses.add(operands[0]);  // debugged this for 2 hours: operands[1] -> operands[0]
                uses.add(operands[2]);
                break;
            case BEQ:
            case BNE:
            case BLT:
            case BGT:
            case BGE:
            case BLE:
                uses.add(operands[0]);
                uses.add(operands[1]);
                break;
            case RETURN:
                uses.add(operands[0]);
                break;
            case CALL:
                uses.addAll(Arrays.stream(operands).skip(2).collect(Collectors.toList()));
                if (operands[1].getIrFunction() != null) {
                    Set<Symbol> funcUses = new HashSet<>(operands[1].getIrFunction().getBasicBlocks().get(0).getInSet());
                    funcUses.removeIf(Symbol::isArgument);
                    uses.addAll(funcUses);
                }
                break;
            case LOAD:
                uses.add(operands[2]);
                break;
        }
        uses.removeIf(Symbol::isConstant);
        return uses;
    }

    public Set<Symbol> getInSet() {
        return inSet;
    }

    public void setInSet(Set<Symbol> inSet) {
        this.inSet = inSet;
    }

    public Set<Symbol> getOutSet() {
        return outSet;
    }

    public void setOutSet(Set<Symbol> outSet) {
        this.outSet = outSet;
    }

    @Override
    public String toString() {
        if (isLabel) {
            return label.getName() + ":";
        } else {
            return opcode.name().toLowerCase() + " " + Arrays.stream(operands).map(symbol -> {
                if (symbol.getSymbolClass() == SymbolClass.ICONST)
                    return String.valueOf(symbol.getIntVal());
                else if (symbol.getSymbolClass() == SymbolClass.FCONST)
                    return String.valueOf(symbol.getFloatVal());
                else
                    return symbol.getName();
            }).collect(Collectors.joining(", "));
        }
    }
}
