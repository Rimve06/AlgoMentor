package com.algomentor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A single node in the demo graph used by BFS/DFS/Dijkstra visualizers.
 *
 * Edge weights were added (additively) to support Dijkstra's shortest-path
 * algorithm without touching how BFS/DFS read neighbors: getNeighbors()
 * still returns the same plain id list it always did, and addNeighbor(id)
 * still works exactly as before (it just now records a default weight of 1
 * behind the scenes).
 */
public class GraphNode {
    private final int id;
    private final double x, y; // fixed layout coordinates for drawing
    private final List<Integer> neighbors = new ArrayList<>();
    private final Map<Integer, Integer> weights = new LinkedHashMap<>();

    public GraphNode(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public List<Integer> getNeighbors() { return neighbors; }

    /** Unchanged behavior: adds a neighbor with an implicit weight of 1. */
    public void addNeighbor(int otherId) { addNeighbor(otherId, 1); }

    /** New: adds a neighbor with an explicit weight, used by Dijkstra. */
    public void addNeighbor(int otherId, int weight) {
        neighbors.add(otherId);
        weights.put(otherId, weight);
    }

    public int getWeight(int neighborId) { return weights.getOrDefault(neighborId, 1); }
}
