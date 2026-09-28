package com.algomentor.algorithm;

import com.algomentor.model.Step;
import java.util.List;

/**
 * Contract for anything that can run itself and hand back a full trace of
 * Steps for later playback. Implemented by both array-based algorithms
 * (sorting/searching) and graph-based algorithms (BFS/DFS) - the interface
 * doesn't care what kind of data structure is underneath.
 */
public interface Traceable {
    /** Runs the algorithm to completion and returns the ordered list of Steps. */
    List<Step> run();

    /** Human-readable name shown in the UI's algorithm picker. */
    String getName();

    /** Big-O time complexity string, shown in the info panel. */
    String getComplexity();

    /** One or two sentence explanation, shown in the info panel. */
    String getDescription();
}