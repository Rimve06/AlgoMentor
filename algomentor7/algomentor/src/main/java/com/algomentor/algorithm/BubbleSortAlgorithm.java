package com.algomentor.algorithm;

public class BubbleSortAlgorithm extends ArrayAlgorithm {

    public BubbleSortAlgorithm(int[] input) {
        super(input);
    }

    @Override
    protected void execute() {
        int n = array.length;
        for (int pass = 0; pass < n - 1; pass++) {
            boolean swapped = false;
            for (int i = 0; i < n - pass - 1; i++) {
                recordCompare(i, i + 1);
                if (array[i] > array[i + 1]) {
                    recordSwap(i, i + 1);
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    @Override
    public String getName() { return "Bubble Sort"; }

    @Override
    public String getComplexity() { return "O(n\u00b2) time, O(1) space"; }

    @Override
    public String getDescription() {
        return "Repeatedly steps through the list, comparing adjacent elements and " +
                "swapping them if they are out of order. Simple but quadratic.";
    }
}