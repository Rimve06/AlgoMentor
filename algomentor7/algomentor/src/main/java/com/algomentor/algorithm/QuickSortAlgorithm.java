package com.algomentor.algorithm;

public class QuickSortAlgorithm extends ArrayAlgorithm {

    public QuickSortAlgorithm(int[] input) {
        super(input);
    }

    @Override
    protected void execute() {
        quickSort(0, array.length - 1);
    }

    private void quickSort(int lo, int hi) {
        if (lo >= hi) return;
        recordPartition(lo, hi);
        int pivotIndex = partition(lo, hi);
        quickSort(lo, pivotIndex - 1);
        quickSort(pivotIndex + 1, hi);
    }

    /** Lomuto partition scheme, using the last element as the pivot. */
    private int partition(int lo, int hi) {
        int pivot = array[hi];
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            recordCompare(j, hi);
            if (array[j] < pivot) {
                i++;
                if (i != j) recordSwap(i, j);
            }
        }
        if (i + 1 != hi) recordSwap(i + 1, hi);
        return i + 1;
    }

    @Override
    public String getName() { return "Quick Sort"; }

    @Override
    public String getComplexity() { return "O(n log n) average, O(n\u00b2) worst case, O(log n) space"; }

    @Override
    public String getDescription() {
        return "Picks a pivot, partitions the array so smaller values end up on its left and " +
                "larger on its right, then recursively sorts each side. Usually the fastest " +
                "general-purpose comparison sort in practice.";
    }
}
