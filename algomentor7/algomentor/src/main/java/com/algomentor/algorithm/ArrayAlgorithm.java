package com.algomentor.algorithm;

import com.algomentor.model.Step;
import com.algomentor.model.StepType;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class shared by every array-based algorithm (sorting and
 * searching). Concrete subclasses only implement {@link #execute()}; this
 * class supplies the working array and small helper methods so every
 * subclass records steps in a consistent shape instead of re-implementing
 * bookkeeping. This is the "advanced OOP" backbone of the algorithm package:
 * an abstract class capturing shared state/behaviour, with each concrete
 * subclass (BubbleSort, MergeSort, BinarySearch) filling in only what makes
 * it unique - a textbook template-method style design.
 */
public abstract class ArrayAlgorithm implements Traceable {
    protected final int[] array;
    protected final List<Step> steps = new ArrayList<>();

    protected ArrayAlgorithm(int[] input) {
        this.array = input.clone();
    }

    /** Subclasses implement the actual algorithm logic here, calling the
     *  protected record* helpers as they go. */
    protected abstract void execute();

    @Override
    public final List<Step> run() {
        steps.clear();
        execute();
        steps.add(new Step(StepType.DONE, List.of(), array.clone(), "Finished."));
        return steps;
    }

    protected void recordCompare(int i, int j) {
        steps.add(new Step(StepType.COMPARE, List.of(i, j), array.clone(),
                "Comparing index " + i + " and " + j));
    }

    protected void recordSwap(int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
        steps.add(new Step(StepType.SWAP, List.of(i, j), array.clone(),
                "Swapped index " + i + " and " + j));
    }

    protected void recordOverwrite(int i, int value) {
        array[i] = value;
        steps.add(new Step(StepType.OVERWRITE, List.of(i), array.clone(),
                "Wrote " + value + " at index " + i));
    }

    protected void recordPartition(int lo, int hi) {
        steps.add(new Step(StepType.PARTITION, List.of(lo, hi), array.clone(),
                "Working on range [" + lo + ", " + hi + "]"));
    }

    protected void recordFound(int index) {
        steps.add(new Step(StepType.MARK_FOUND, List.of(index), array.clone(),
                "Target found at index " + index));
    }
}