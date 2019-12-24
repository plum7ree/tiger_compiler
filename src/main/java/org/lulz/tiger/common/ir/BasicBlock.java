package org.lulz.tiger.common.ir;

import org.lulz.tiger.common.symbol.Symbol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BasicBlock extends ArrayList<IRInsn> {
    private Set<Symbol> inSet;
    private Set<Symbol> outSet;

    public IRInsn getLeader() {
        return get(0);
    }

    public IRInsn getLast() {
        return get(size() - 1);
    }

    public void initializeSets() {
        Set<Symbol> lastSet = new HashSet<>();
        inSet = lastSet;
        for (IRInsn insn : this) {
            insn.setInSet(lastSet);
            lastSet = new HashSet<>();
            insn.setOutSet(lastSet);
        }
        outSet = lastSet;
    }

    public Set<Symbol> getInSet() {
        return inSet;
    }

    public Set<Symbol> getOutSet() {
        return outSet;
    }

    @Override
    public String toString() {
        // for use with CFG exporter. \\l is left-aligned newline
        StringBuilder sb = new StringBuilder();
        for (IRInsn insn : this) {
            if (!insn.isLabel() && insn.getInSet() != null) {
                sb.append("- ").append(insn.getInSet()).append("\\l");
            }
            sb.append(insn).append("\\l");
        }
        if (outSet != null) {
            sb.append("- ").append(outSet).append("\\l");
        }
        return sb.toString();
    }
}
