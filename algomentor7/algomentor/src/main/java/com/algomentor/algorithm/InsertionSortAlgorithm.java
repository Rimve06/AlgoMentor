package com.algomentor.algorithm;

public class InsertionSortAlgorithm extends ArrayAlgorithm {

    public InsertionSortAlgorithm(int[] input) {
        super(input);
    }

    @Override
    protected void execute() {
        int n = array.length;
        for (int i = 1; i < n; i++) {
            int j = i;
            while (j > 0) {
                recordCompare(j - 1, j);
                if (array[j - 1] > array[j]) {
                    recordSwap(j - 1, j);
                    j--;
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public String getName() { return "Insertion Sort"; }

    @Override
    public String getComplexity() { return "O(n\u00b2) worst/avg, O(n) best (nearly-sorted input), O(1) space"; }

    @Override
    public String getDescription() {
        return "Builds the sorted array one element at a time, inserting each new value " +
                "into its correct position among the already-sorted elements to its left. " +
                "Very fast on nearly-sorted data.";
    }
}
