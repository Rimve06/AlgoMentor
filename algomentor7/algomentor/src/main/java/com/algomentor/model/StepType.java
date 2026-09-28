package com.algomentor.model;

/**
 * The vocabulary of "things that can happen" during an algorithm's execution.
 * Every visualizer emits a sequence of Step objects built from these types;
 * the renderer only needs to know how to draw each type, not how the
 * algorithm itself works. This decoupling is what lets one playback engine
 * (see controller.MainController) drive five completely different algorithms.
 */
public enum StepType {
    COMPARE,     // two indices/nodes are being compared
    SWAP,        // two indices were swapped
    OVERWRITE,   // a value at an index was overwritten (used by merge sort)
    VISIT,       // a graph/tree node was visited
    ENQUEUE,     // a node was pushed onto the frontier (BFS queue / DFS stack)
    DEQUEUE,     // a node was popped from the frontier
    MARK_FOUND,  // final answer found (e.g., binary search target, target node)
    PARTITION,   // a sub-range boundary was established (merge sort divide step)
    DONE         // algorithm finished
}