package com.algomentor.algorithm;

import com.algomentor.model.DemoGraph;
import com.algomentor.model.GraphNode;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class DFSAlgorithm extends GraphAlgorithm {

    public DFSAlgorithm(DemoGraph graph, int startId) {
        super(graph, startId);
    }

    @Override
    protected void execute() {
        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();

        stack.push(startId);
        recordEnqueue(startId);

        while (!stack.isEmpty()) {
            int current = stack.pop();
            recordDequeue(current);
            if (visited.contains(current)) continue;
            visited.add(current);
            recordVisit(current);

            GraphNode node = graph.get(current);
            for (int neighbor : node.getNeighbors()) {
                if (!visited.contains(neighbor)) {
                    stack.push(neighbor);
                    recordEnqueue(neighbor);
                }
            }
        }
    }

    @Override
    public String getName() { return "Depth-First Search"; }

    @Override
    public String getComplexity() { return "O(V + E) time, O(V) space"; }

    @Override
    public String getDescription() {
        return "Explores a graph by diving as deep as possible down one branch " +
                "using a stack, before backtracking.";
    }
}