package org.lulz.tiger.backend.codegen;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.lulz.tiger.backend.liveness.Web;
import org.lulz.tiger.common.ir.IRInsn;
import org.lulz.tiger.common.symbol.Symbol;

import java.util.Optional;

public class BriggsRegisterAllocator implements RegisterAllocator {

    @Override
    public MIPSRegister getRegister(Symbol symbol, IRInsn insn) {
        if (symbol.isArgument()) {
            return MIPSCodeGenerator.ARG_REGS[symbol.getFrameIndex() / 4];
        }
        SimpleGraph<Web, DefaultEdge> graph = insn.getFunction().getColoredGraph();
        Optional<Web> web_opt = graph.vertexSet().stream().filter(v -> v.getSymbol() == symbol && v.getRange().contains(insn)).findAny();
        if (web_opt.isEmpty()) {
            return null;
        }
        return web_opt.get().getRegister();
    }
}
