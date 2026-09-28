package com.algomentor.algorithm;

import com.algomentor.model.DemoGraph;
import com.algomentor.model.GraphNode;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class BFSAlgorithm extends GraphAlgorithm {

    public BFSAlgorithm(DemoGraph graph, int startId) {
        super(graph, startId);
    }

    @Override
    protected void execute() {
        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        queue.add(startId);
        recordEnqueue(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            recordDequeue(current);
            recordVisit(current);

            GraphNode node = graph.get(current);
            for (int neighbor : node.getNeighbors()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    recordEnqueue(neighbor);
                }
            }
        }
    }

    @Override
    public String getName() { return "Breadth-First Search"; }

    @Override
    public String getComplexity() { return "O(V + E) time, O(V) space"; }

    @Override
    public String getDescription() {
        return "Explores a graph level by level using a queue, visiting all neighbors " +
                "of a node before moving further out.";
    }
}