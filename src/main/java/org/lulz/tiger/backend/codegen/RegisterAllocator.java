package org.lulz.tiger.backend.codegen;

import org.lulz.tiger.common.ir.IRInsn;
import org.lulz.tiger.common.symbol.Symbol;

public interface RegisterAllocator {
    MIPSRegister getRegister(Symbol symbol, IRInsn insn);
}
