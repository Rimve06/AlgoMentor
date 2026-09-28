package com.algomentor.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small fixed demo graph (8 nodes) with a hand-picked layout so BFS/DFS/
 * Dijkstra traversal can be drawn on a canvas without a force-directed
 * layout engine. Undirected, weighted - BFS/DFS ignore the weights (they
 * only call getNeighbors()), Dijkstra uses them for shortest-path relaxation.
 */
public class DemoGraph {
    private final Map<Integer, GraphNode> nodes = new LinkedHashMap<>();

    public DemoGraph() {
        addNode(0, 350, 60);
        addNode(1, 180, 160);
        addNode(2, 520, 160);
        addNode(3, 80, 280);
        addNode(4, 280, 280);
        addNode(5, 420, 280);
        addNode(6, 620, 280);
        addNode(7, 350, 400);

        edge(0, 1, 4); edge(0, 2, 2);
        edge(1, 3, 5); edge(1, 4, 1);
        edge(2, 5, 3); edge(2, 6, 6);
        edge(4, 7, 2); edge(5, 7, 4);
    }

    private void addNode(int id, double x, double y) {
        nodes.put(id, new GraphNode(id, x, y));
    }

    /** Unweighted convenience overload (kept for backward compatibility). */
    private void edge(int a, int b) { edge(a, b, 1); }

    private void edge(int a, int b, int weight) {
        nodes.get(a).addNeighbor(b, weight);
        nodes.get(b).addNeighbor(a, weight);
    }

    public Map<Integer, GraphNode> getNodes() { return nodes; }
    public GraphNode get(int id) { return nodes.get(id); }
    public int size() { return nodes.size(); }
}
