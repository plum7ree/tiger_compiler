package org.lulz.tiger.backend.liveness;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.io.*;
import org.lulz.tiger.backend.codegen.MIPSCodeGenerator;
import org.lulz.tiger.backend.coloring.RegColoring;
import org.lulz.tiger.common.ir.BasicBlock;
import org.lulz.tiger.common.ir.IRFunction;
import org.lulz.tiger.common.ir.IRInsn;
import org.lulz.tiger.common.ir.IRListing;
import org.lulz.tiger.common.symbol.Symbol;

import java.io.File;
import java.util.*;

public class LivenessAnalyzer implements Runnable {
    private IRListing listing;

    public LivenessAnalyzer(IRListing listing) {
        this.listing = listing;
    }

    @Override
    public void run() {
        constructLiveSets();
        listing.getFunctions().forEach(this::analyzeFunction);
    }

    private void constructLiveSets() {
        listing.getFunctions().forEach(f -> f.getBasicBlocks().forEach(BasicBlock::initializeSets));

        boolean modified;
        do {
            modified = false;

            for (IRFunction function : listing.getFunctions()) {
                for (BasicBlock block : function.getBasicBlocks()) {
                    ListIterator<IRInsn> li = block.listIterator(block.size());
                    while (li.hasPrevious()) {  // reverse order
                        IRInsn insn = li.previous();
                        Set<Symbol> newSet = new HashSet<>(insn.getOutSet());
                        newSet.removeAll(insn.getDefs());
                        newSet.addAll(insn.getUses());
                        modified |= insn.getInSet().addAll(newSet);
                    }
                }

                for (BasicBlock block : function.getBasicBlocks()) {
                    for (BasicBlock succ : Graphs.successorListOf(function.getCfg(), block)) {
                        modified |= block.getOutSet().addAll(succ.getInSet());
                    }
                }
            }
        } while (modified);
    }

    private void analyzeFunction(IRFunction function) {
        // 1. Generate webs within each block
        Map<BasicBlock, Map<Symbol, Web>> inWebs = new HashMap<>();
        Map<BasicBlock, Map<Symbol, Web>> outWebs = new HashMap<>();
        Map<BasicBlock, Set<Web>> blockWebs = new HashMap<>();
        Set<Set<Web>> interference = new HashSet<>();

        for (BasicBlock block : function.getBasicBlocks()) {
            // Build liveMap for this block

            int blockSize = block.size( );
            if (blockSize < 1) {
                throw new RuntimeException("empty block");
            }

            Set<Web> blockWeb = new HashSet<>();
            Map<Symbol, Web> liveWeb = new HashMap<>();
            Map<Symbol, Web> inWeb = new HashMap<>();

            // First Instruction
            IRInsn insn = block.get(0);
            Symbol funcSymbol = insn.getFunction().getSymbol();
            Set<Web> overlap = new HashSet<>();
            Set<Symbol> defSet, outSet;
            Set<Symbol> inSet = insn.getInSet();

            for (Symbol inSymbol: inSet) {
                if (inSymbol.getFunction() == funcSymbol && !inSymbol.isArgument()) {
                    Web web = new Web(inSymbol);
                    inWeb.put(inSymbol, web);
                    liveWeb.put(inSymbol, web);
                    web.getRange().add(insn);
                }
            }
            interference.add(overlap);

            // Rest of Instructions
            for (IRInsn irInsn : block) {
                insn = irInsn;
                funcSymbol = insn.getFunction().getSymbol();
                overlap = new HashSet<>();
                defSet = insn.getDefs();
                inSet = insn.getInSet();
                outSet = insn.getOutSet();

                // add current instruction to the valid variable range
                for (Symbol inSymbol : inSet) {
                    if (inSymbol.getFunction() == funcSymbol && !inSymbol.isArgument()) {
                        Web web = liveWeb.get(inSymbol);
                        web.getRange().add(insn);
                        if (outSet.contains(inSymbol))
                            overlap.add(web);
                    }
                }
                for (Symbol defSymbol : defSet) {
                    if (defSymbol.getFunction() == funcSymbol && !defSymbol.isArgument()) {
                        Web web = liveWeb.get(defSymbol);
                        if (web == null) {
                            web = new Web(defSymbol);
                            liveWeb.put(defSymbol, web);
                        }
                        web.getRange().add(insn);
                        web.incSpillCost();
                        overlap.add(web);
                    }
                }
                for (Symbol useSymbol : insn.getUses()) {
                    if (useSymbol.getFunction() == funcSymbol && !useSymbol.isArgument()) {
                        Web web = liveWeb.get(useSymbol);
                        web.incSpillCost();
                    }
                }
                Iterator<Symbol> liveIterator = liveWeb.keySet().iterator();
                while (liveIterator.hasNext()) {
                    Symbol liveSymbol = liveIterator.next();
                    if (liveSymbol.getFunction() == funcSymbol && !liveSymbol.isArgument()) {
                        if (!outSet.contains(liveSymbol)) {
                            Web killWeb = liveWeb.get(liveSymbol);
                            blockWeb.add(killWeb);
                            liveIterator.remove();
                        }
                    }
                }
                interference.add(overlap);
            }
            inWebs.put(block, inWeb);
            outWebs.put(block, liveWeb);
            blockWebs.put(block, blockWeb);
        }

        // 2. Merge blocks' in/out webs according to the control flow
        // Setup Disjoint Sets
        DisjointWeb disjointWeb = new DisjointWeb();
        for (Map<Symbol, Web> inWeb: inWebs.values()) {
            disjointWeb.addWebs(inWeb.values());
        }
        for (Map<Symbol, Web> outWeb: outWebs.values()) {
            disjointWeb.addWebs(outWeb.values());
        }
        // Union appropriate sets
        for (BasicBlock block : function.getBasicBlocks()) {
            Map<Symbol, Web> outCurr = outWebs.get(block);
            for (BasicBlock succ: Graphs.successorListOf(function.getCfg(), block)) {
                Map<Symbol, Web> inSucc = inWebs.get(succ);
                for (Symbol inSymbol : inSucc.keySet()) {
                    Web outWeb = outCurr.get(inSymbol);
                    Web inWeb = inSucc.get(inSymbol);
                    disjointWeb.union(inWeb, outWeb);
                }
            }
        }

        Set<Web> vertices = new HashSet<>();
        // Replace union sets with one matching web
        for (Map<Symbol, Web> inWeb: inWebs.values()) {
            for (Map.Entry<Symbol, Web> entry: inWeb.entrySet()) {
                Web newWeb = disjointWeb.find(entry.getValue());
                entry.setValue(newWeb);
                vertices.add(newWeb);
            }
        }
        for (Set<Web> blockWeb: blockWebs.values()) {
            Iterator<Web> blockWebIterator = blockWeb.iterator();
            Set<Web> topWebs = new HashSet<>();
            while (blockWebIterator.hasNext()) {
                Web web = blockWebIterator.next();
                Web topWeb = disjointWeb.find(web);
                if (topWeb != null) {
                    if (topWeb != web) {
                        blockWebIterator.remove();
                        topWebs.add(topWeb);
                        vertices.add(topWeb);
                    }
                } else {
                    vertices.add(web);
                }
            }
            blockWeb.addAll(topWebs);
        }
        for (Map<Symbol, Web> outWeb: outWebs.values()) {
            for (Map.Entry<Symbol, Web> entry: outWeb.entrySet()) {
                Web newWeb = disjointWeb.find(entry.getValue());
                entry.setValue(newWeb);
                vertices.add(newWeb);
            }
        }

        for (Set<Web> overlap: interference) {
            Iterator<Web> overlapIterator = overlap.iterator();
            Set<Web> newWebs = new HashSet<>();
            while (overlapIterator.hasNext()) {
                Web web = overlapIterator.next();
                Web newWeb = disjointWeb.find(web);
                if (newWeb != null && newWeb != web) {
                    overlapIterator.remove();
                    newWebs.add(newWeb);
                }
            }
            overlap.addAll(newWebs);
        }

        // 3. Build Graph
        SimpleGraph<Web, DefaultEdge> simpleGraph = new SimpleGraph<>(DefaultEdge.class);
        // Add vertices
        for (Web web: vertices) {
            simpleGraph.addVertex(web);
        }
        // Add edges
        for (Set<Web> overlap: interference) {
            List<Web> overlapList = new ArrayList<>(overlap);
            int overlapListSize = overlapList.size();
            for (int i = 0; i < overlapListSize - 1; i++) {
                for (int j = i + 1; j < overlapListSize; j++) {
                    simpleGraph.addEdge(overlapList.get(i), overlapList.get(j));
                }
            }
        }

        // DEBUG: WEB IN/OUT SETS
        int count = 1;
        for (BasicBlock block : function.getBasicBlocks()) {
            System.out.println("===== Block: " + count + " =====");
            for (IRInsn instr: block) {
                System.out.println(instr);
            }

            Map<Symbol, Web> inWeb = inWebs.get(block);
            System.out.println("#### IN SET ####");
            for (Web web: inWeb.values()) {
                System.out.println("Symbol: " + web.getSymbol() + ", Spill: " + web.getSpillCost() + ", Hash: "  + web.hashCode() + ", Range: "  + web.getRange().size());
            }

            Set<Web> liveWeb = blockWebs.get(block);
            System.out.println("#### INTERNAL ####");
            for (Web web: liveWeb) {
                System.out.println("Symbol: " + web.getSymbol() + ", Spill: " + web.getSpillCost() + ", Hash: "  + web.hashCode() + ", Range: "  + web.getRange().size());
            }

            Map<Symbol, Web> outWeb = outWebs.get(block);
            System.out.println("#### OUT SET ####");
            for (Web web: outWeb.values()) {
                System.out.println("Symbol: " + web.getSymbol() + ", Spill: " + web.getSpillCost() + ", Hash: "  + web.hashCode() + ", Range: "  + web.getRange().size());
            }
            System.out.println();
            count++;
        }

        int NUM_REG_AVAIL = MIPSCodeGenerator.GP_REGS.length;
        RegColoring rc = new RegColoring(simpleGraph, NUM_REG_AVAIL);
        rc.run();

        function.setColoredGraph(simpleGraph);

        // DEBUG: COLORING
        System.out.println("Color Nodes: ");
        simpleGraph.vertexSet().stream().filter(v -> v.getColor() != RegColoring.NO_COLOR)
                .forEach(v -> System.out.println(v.getSymbol() + ": " + v.getColor()));

        System.out.println("Spilled Nodes: ");
        simpleGraph.vertexSet().stream().filter(v -> v.getColor() == RegColoring.NO_COLOR)
                .forEach(v -> System.out.println(v.getSymbol()));
        System.out.println();

    }
}
