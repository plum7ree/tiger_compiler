package org.lulz.tiger.backend.codegen;

import org.lulz.tiger.common.ir.IRInsn;
import org.lulz.tiger.common.symbol.Symbol;

public class NaiveRegisterAllocator implements RegisterAllocator {

    @Override
    public MIPSRegister getRegister(Symbol symbol, IRInsn insn) {
        if (symbol.isArgument()) {
            return MIPSCodeGenerator.ARG_REGS[symbol.getFrameIndex() / 4];
        }
        return null;
    }
}
