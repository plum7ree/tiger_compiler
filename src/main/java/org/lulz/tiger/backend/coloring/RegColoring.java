package org.lulz.tiger.backend.coloring;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.lulz.tiger.backend.liveness.Web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class RegColoring implements Runnable {
    public static final int NO_COLOR = -1;
    private SimpleGraph<Web, DefaultEdge> graph;
    private int N;

    public RegColoring(SimpleGraph<Web, DefaultEdge> graph, int numRegAvailable) {
        this.graph = graph;
        this.N = numRegAvailable;
    }

    @Override
    public void run() {
        @SuppressWarnings("unchecked")
        SimpleGraph<Web, DefaultEdge> spilledGraph = (SimpleGraph<Web, DefaultEdge>) graph.clone();

        while (true) {
            Stack<Web> stack = pushToStack(spilledGraph);
            Web spilledNode = tryColoring(stack);

            if (spilledNode == null) {
                break;
            }
            spilledGraph.removeVertex(spilledNode);
        }
    }

    Stack<Web> pushToStack(SimpleGraph<Web, DefaultEdge> spilledGraph) {
        Stack<Web> stack = new Stack<>();
        List<Web> vertexList = new ArrayList<>(spilledGraph.vertexSet());

        //Remove nodes that have degree < N
        List<Web> toRemove = vertexList.stream().filter(v -> spilledGraph.degreeOf(v) < N)
                .collect(Collectors.toList());
        stack.addAll(toRemove);
        vertexList.removeAll(toRemove);

        //2.Remove that node and push it on the stack
        Collections.sort(vertexList);
        stack.addAll(vertexList);

        return stack;
    }

    Web tryColoring(Stack<Web> stack) {
        graph.vertexSet().forEach(w -> w.setColor(NO_COLOR));   // reset all colors

        while (!stack.empty()) {
            Web v = stack.pop();

            boolean[] neighborHasColor = new boolean[N];
            for (Web adjv : Graphs.neighborListOf(graph, v)) {
            //System.out.println("v: " + adjv + "c: " + adjv.getColor());
                if (adjv.getColor() != NO_COLOR) {
                    neighborHasColor[adjv.getColor()] = true;
                }
            }

            int temp_color = NO_COLOR;
            for (int i = 0; i < neighborHasColor.length; i++) {
                if (!neighborHasColor[i]) {
                    temp_color = i;
                    break;
                }
            }

            if (temp_color == NO_COLOR) {
                // no color available, spill!
                return v;
            }
            v.setColor(temp_color);
        }
        return null;
    }

}
