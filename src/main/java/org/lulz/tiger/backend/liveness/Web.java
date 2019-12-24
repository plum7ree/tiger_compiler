package org.lulz.tiger.backend.liveness;

import org.lulz.tiger.backend.codegen.MIPSCodeGenerator;
import org.lulz.tiger.backend.codegen.MIPSRegister;
import org.lulz.tiger.backend.coloring.RegColoring;
import org.lulz.tiger.common.ir.IRInsn;
import org.lulz.tiger.common.symbol.Symbol;

import java.util.HashSet;
import java.util.Set;

public class Web implements Comparable<Web> {
    private Symbol symbol;
    private Set<IRInsn> range;
    private int color = RegColoring.NO_COLOR;
    private int spillCost;
    private MIPSRegister register;

    public Web(Symbol symbol) {
        this.symbol = symbol;
        this.range = new HashSet<>();
        this.spillCost = 0;
    }

    public Web(Symbol symbol, Set<IRInsn> range) {
        this.symbol = symbol;
        this.range = range;
        this.spillCost = 0;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Set<IRInsn> getRange() {
        return range;
    }

    public int getSpillCost() {
        return spillCost;
    }

    public void incSpillCost() {
        this.spillCost++;
    }

    public void addSpillCost(int spillCost) {
        this.spillCost += spillCost;
    }

    public void setSpillCost(int spillCost) {
        this.spillCost = spillCost;
    }

    public int getColor() {
        return color;
    }

    public MIPSRegister getRegister() {
        return register;
    }

    public void setColor(int color) {
        this.color = color;
        this.register = color == -1 ? null : MIPSCodeGenerator.GP_REGS[color];
    }

    @Override
    public int compareTo(Web other) {
        return Integer.compare(this.getSpillCost(), other.getSpillCost());
    }

    @Override
    public String toString() {
        return this.symbol.toString() + ": " + this.spillCost;
    }

}
