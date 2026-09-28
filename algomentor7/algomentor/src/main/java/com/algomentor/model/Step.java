package com.algomentor.model;

import java.util.Collections;
import java.util.List;

/**
 * One immutable frame of an algorithm's execution trace.
 *
 * Design note: rather than one Step subclass per algorithm, every algorithm
 * emits the SAME shape of object, and callers interpret {@code indices}
 * according to {@code type}. E.g. for SWAP, indices = [i, j]; for VISIT,
 * indices = [nodeId]. This keeps the renderer (CanvasRenderer) generic:
 * it switches on StepType, not on which algorithm produced the step.
 *
 * A Step also carries an optional snapshot of the full array state
 * (used by sorting algorithms) so the renderer never has to replay
 * history to know "what does the array look like right now" - it can
 * just jump to any step index (this is what makes step-back / scrubbing
 * possible without re-running the algorithm).
 */
public final class Step {
    private final StepType type;
    private final List<Integer> indices;   // meaning depends on `type`
    private final int[] arraySnapshot;     // null for graph/tree algorithms
    private final String description;      // human-readable caption for the UI

    public Step(StepType type, List<Integer> indices, int[] arraySnapshot, String description) {
        this.type = type;
        this.indices = indices == null ? Collections.emptyList() : List.copyOf(indices);
        this.arraySnapshot = arraySnapshot == null ? null : arraySnapshot.clone();
        this.description = description;
    }

    public StepType getType() { return type; }
    public List<Integer> getIndices() { return indices; }
    public int[] getArraySnapshot() { return arraySnapshot == null ? null : arraySnapshot.clone(); }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "Step{" + type + ", indices=" + indices + ", desc='" + description + "'}";
    }
}