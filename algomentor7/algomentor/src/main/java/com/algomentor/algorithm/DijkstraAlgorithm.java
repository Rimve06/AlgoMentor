package com.algomentor.algorithm;

import com.algomentor.model.DemoGraph;
import com.algomentor.model.GraphNode;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dijkstra's single-source shortest path algorithm over the weighted
 * DemoGraph. Reuses GraphAlgorithm's existing recordEnqueue/recordDequeue/
 * recordVisit hooks (and therefore CanvasRenderer, unmodified) - "enqueue"
 * means "relaxed into the frontier with a candidate distance", "visit"
 * means "distance finalized (popped with the smallest tentative distance)".
 */
public class DijkstraAlgorithm extends GraphAlgorithm {

    public DijkstraAlgorithm(DemoGraph graph, int startId) {
        super(graph, startId);
    }

    @Override
    protected void execute() {
        Map<Integer, Integer> dist = new HashMap<>();
        for (int id : graph.getNodes().keySet()) dist.put(id, Integer.MAX_VALUE);
        dist.put(startId, 0);

        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));
        pq.add(new int[]{startId, 0});
        recordEnqueue(startId);

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int u = top[0], d = top[1];
            if (d > dist.get(u)) continue; // stale entry, already finalized with a better distance
            recordDequeue(u);
            steps.add(new com.algomentor.model.Step(
                    com.algomentor.model.StepType.VISIT, java.util.List.of(u), null,
                    "Finalized node " + u + " with shortest distance " + d));

            GraphNode node = graph.get(u);
            for (int v : node.getNeighbors()) {
                int weight = node.getWeight(v);
                int newDist = d + weight;
                if (newDist < dist.get(v)) {
                    dist.put(v, newDist);
                    pq.add(new int[]{v, newDist});
                    steps.add(new com.algomentor.model.Step(
                            com.algomentor.model.StepType.ENQUEUE, java.util.List.of(v), null,
                            "Relaxed edge " + u + " -> " + v + " (weight " + weight + "): new candidate distance " + newDist));
                }
            }
        }
    }

    @Override
    public String getName() { return "Dijkstra's Algorithm"; }

    @Override
    public String getComplexity() { return "O((V + E) log V) time with a priority queue, O(V) space"; }

    @Override
    public String getDescription() {
        return "Finds the shortest distance from a start node to every other node in a " +
                "weighted graph by always finalizing the closest not-yet-settled node next.";
    }
}
