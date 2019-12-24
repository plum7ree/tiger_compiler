package org.lulz.tiger.backend.coloring;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Before;
import org.junit.Test;
import org.lulz.tiger.backend.liveness.Web;

import java.util.*;

public class ColorAllocTest {
    RegColoring rc;
    SimpleGraph<Web, DefaultEdge> originalGraph;
    HashMap<Web, Integer> debugWeb;
    //int N = 4;
    //int N = 3;
    int N = 2;

    //ArrayList<LinkedList<Integer>> g = g1();
    ArrayList<LinkedList<Integer>> g = g2();

    //HashMap<Integer, Integer> sc = sc1();
    //HashMap<Integer, Integer> sc = sc2();
    //HashMap<Integer, Integer> sc = sc3();
    HashMap<Integer, Integer> sc = sc4();

    public ArrayList<LinkedList<Integer>> g1() {
        // graph similar to http://web.cecs.pdx.edu/~mperkows/temp/register-allocation.pdf
        // but a and b is connected
        ArrayList<LinkedList<Integer>> g = new ArrayList<>();
        g.add(new LinkedList<>(Arrays.asList(1, 2, 5)));
        g.add(new LinkedList<>(Arrays.asList(0, 2, 4, 5)));
        g.add(new LinkedList<>(Arrays.asList(0, 1, 3, 4, 5)));
        g.add(new LinkedList<>(Arrays.asList(2, 4, 5)));
        g.add(new LinkedList<>(Arrays.asList(1, 2, 3, 5)));
        g.add(new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4)));
        return g;
    }

    public ArrayList<LinkedList<Integer>> g2() {
        // 0 --- 1 ---- 5
        //       |    / | \
        //       |  /   |  \
        //       2 ---- 3 -- 4
        ArrayList<LinkedList<Integer>> g = new ArrayList<>();
        g.add(new LinkedList<>(Arrays.asList(1)));
        g.add(new LinkedList<>(Arrays.asList(0, 2, 5)));
        g.add(new LinkedList<>(Arrays.asList(1, 3, 5)));
        g.add(new LinkedList<>(Arrays.asList(2, 4, 5)));
        g.add(new LinkedList<>(Arrays.asList(3, 5)));
        g.add(new LinkedList<>(Arrays.asList(1, 2, 3, 4)));
        return g;
    }

    public HashMap<Integer, Integer> sc1() {
        HashMap<Integer, Integer> hm = new HashMap<>();
        hm.put(0, 1);
        hm.put(1, 1);
        hm.put(2, 1);
        hm.put(3, 1);
        hm.put(4, 1);
        hm.put(5, 1);
        return hm;
    }

    public HashMap<Integer, Integer> sc2() {
        HashMap<Integer, Integer> hm = new HashMap<>();
        hm.put(0, 6);
        hm.put(1, 5);
        hm.put(2, 4);
        hm.put(3, 3);
        hm.put(4, 2);
        hm.put(5, 1);
        return hm;
    }

    public HashMap<Integer, Integer> sc3() {
        HashMap<Integer, Integer> hm = new HashMap<>();
        hm.put(0, 2);
        hm.put(1, 1);
        hm.put(2, 2);
        hm.put(3, 1);
        hm.put(4, 2);
        hm.put(5, 1);
        return hm;
    }

    public HashMap<Integer, Integer> sc4() {
        HashMap<Integer, Integer> hm = new HashMap<>();
        hm.put(0, 10);
        hm.put(1, 10);
        hm.put(2, 10);
        hm.put(3, 1);
        hm.put(4, 1);
        hm.put(5, 2);
        return hm;
    }

    public SimpleGraph<Web, DefaultEdge> ListToGraphConverter(ArrayList<LinkedList<Integer>> l, HashMap<Integer, Integer> spillCost) {
        debugWeb = new HashMap<>();
        SimpleGraph<Web, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        ArrayList<Web> vertices = new ArrayList<>();

        spillCost.keySet().forEach(k -> {
            Web v = new Web(null,null);
            v.setSpillCost(spillCost.get(k));
            vertices.add(v);
            g.addVertex(v);
            debugWeb.put(v, k);
        });

        for (int v = 0; v < l.size(); v++) {
            Web v1 = vertices.get(v);
            l.get(v).forEach(i -> {
                Web v2 = vertices.get(i);
                if (!g.containsEdge(v1, v2)) {
                    g.addEdge(v1, v2);
                }
            });
        }
        return g;
    }

    @Before
    public void setup() {
        originalGraph = ListToGraphConverter(g, sc);
        rc = new RegColoring(originalGraph, N);
    }

    @Test
    public void testColorGraph() {
        rc.run();

        System.out.println("Color Nodes: ");
        originalGraph.vertexSet().stream().filter(v -> v.getColor() != RegColoring.NO_COLOR)
                .forEach(v -> System.out.println(debugWeb.get(v) + ": " + v.getColor()));

        System.out.println("Spilled Nodes: ");
        originalGraph.vertexSet().stream().filter(v -> v.getColor() == RegColoring.NO_COLOR)
                .forEach(v -> System.out.println(debugWeb.get(v)));
    }

    @Test
    public void testPushToStack() {
        Stack<Web> st = rc.pushToStack(originalGraph);
        System.out.println("stack top");
        while (!st.empty()) {
            Web v = st.pop();
            System.out.println(debugWeb.get(v) + ": " + v.getColor());
        }
        System.out.println("stack bottom");
    }

    @Test
    public void testTryColoring() {
        Stack<Web> st = rc.pushToStack(originalGraph);
        System.out.println("Stack: bottom");
        st.forEach(v -> System.out.println(debugWeb.get(v) + ": " + v.getColor()));
        System.out.println("Stack: top");

        Web spillNode = rc.tryColoring(st);

        System.out.println("Color: ");
        originalGraph.vertexSet().stream().filter(v -> v.getColor() != RegColoring.NO_COLOR)
                .forEach(v -> System.out.println("v: " + debugWeb.get(v) + " , c: " + v.getColor()));
        System.out.println("spill node: " + debugWeb.get(spillNode));
    }
}
