package org.lulz.tiger.backend.cfg;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.lulz.tiger.common.ir.BasicBlock;
import org.lulz.tiger.common.ir.IRFunction;
import org.lulz.tiger.common.ir.IRInsn;
import org.lulz.tiger.common.ir.IRListing;

import java.util.List;

public class ControlFlowAnalyzer implements Runnable {
    private IRListing listing;

    public ControlFlowAnalyzer(IRListing listing) {
        this.listing = listing;
    }

    @Override
    public void run() {
        listing.getFunctions().forEach(this::analyzeFunction);
    }

    private void analyzeFunction(IRFunction function) {
        List<IRInsn> insns = function.getInstructions();
        if (insns.isEmpty()) {
            throw new RuntimeException("empty function");
        }

        /* leader analysis */
        boolean nextIsLeader = true; // first instruction is a leader
        for (IRInsn insn : insns) {
            if (nextIsLeader) {
                insn.setLeader(true);
                nextIsLeader = false;
            }
            IRInsn branchTarget = insn.getBranchTarget();
            if (branchTarget != null) {
                branchTarget.setLeader(true);
                nextIsLeader = true; // instruction after branch is a leader
            }
        }

        /* split basic blocks */
        BasicBlock currentBlock = new BasicBlock();
        for (IRInsn insn : insns) {
            if (insn.isLeader() && !currentBlock.isEmpty()) {
                function.addBasicBlock(currentBlock);
                currentBlock = new BasicBlock();
            }
            currentBlock.add(insn);
        }
        if (!currentBlock.isEmpty()) {
            function.addBasicBlock(currentBlock);
        }

        /* cfg generation */
        Graph<BasicBlock, DefaultEdge> graph = new DirectedPseudograph<>(DefaultEdge.class);
        function.getBasicBlocks().forEach(graph::addVertex); // populate nodes

        BasicBlock prevBlock = null;
        for (BasicBlock block : function.getBasicBlocks()) { // populate edges
            if (prevBlock != null && prevBlock.getLast().isFallThrough()) {
                graph.addEdge(prevBlock, block);
            }
            IRInsn branchTarget = block.getLast().getBranchTarget();
            if (branchTarget != null) {
                BasicBlock targetBlock = function.getBlockByLeader(branchTarget);
                graph.addEdge(block, targetBlock);
            }
            prevBlock = block;
        }
        function.setCfg(graph);
    }
}
