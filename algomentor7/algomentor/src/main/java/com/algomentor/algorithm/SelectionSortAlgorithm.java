package com.algomentor.algorithm;

public class SelectionSortAlgorithm extends ArrayAlgorithm {

    public SelectionSortAlgorithm(int[] input) {
        super(input);
    }

    @Override
    protected void execute() {
        int n = array.length;
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                recordCompare(minIndex, j);
                if (array[j] < array[minIndex]) minIndex = j;
            }
            if (minIndex != i) recordSwap(i, minIndex);
        }
    }

    @Override
    public String getName() { return "Selection Sort"; }

    @Override
    public String getComplexity() { return "O(n\u00b2) time (always), O(1) space"; }

    @Override
    public String getDescription() {
        return "Repeatedly scans the unsorted portion of the array to find the smallest " +
                "remaining value, then swaps it into place. Fewer swaps than bubble sort, but " +
                "no early-exit best case.";
    }
}
