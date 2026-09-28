package com.algomentor.algorithm;

import com.algomentor.model.DemoGraph;
import com.algomentor.model.Step;
import com.algomentor.model.StepType;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for traversal algorithms over {@link DemoGraph}. Mirrors
 * ArrayAlgorithm's template-method shape but for graph indices/nodeIds
 * instead of array positions, demonstrating polymorphism: MainController
 * treats every Traceable (array or graph based) identically via the
 * common interface.
 */
public abstract class GraphAlgorithm implements Traceable {
    protected final DemoGraph graph;
    protected final int startId;
    protected final List<Step> steps = new ArrayList<>();

    protected GraphAlgorithm(DemoGraph graph, int startId) {
        this.graph = graph;
        this.startId = startId;
    }

    protected abstract void execute();

    @Override
    public final List<Step> run() {
        steps.clear();
        execute();
        steps.add(new Step(StepType.DONE, List.of(), null, "Traversal complete."));
        return steps;
    }

    protected void recordEnqueue(int nodeId) {
        steps.add(new Step(StepType.ENQUEUE, List.of(nodeId), null, "Added node " + nodeId + " to frontier"));
    }

    protected void recordDequeue(int nodeId) {
        steps.add(new Step(StepType.DEQUEUE, List.of(nodeId), null, "Removed node " + nodeId + " from frontier"));
    }

    protected void recordVisit(int nodeId) {
        steps.add(new Step(StepType.VISIT, List.of(nodeId), null, "Visiting node " + nodeId));
    }
}