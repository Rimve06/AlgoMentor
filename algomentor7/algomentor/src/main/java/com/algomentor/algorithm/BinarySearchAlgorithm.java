package com.algomentor.algorithm;

public class BinarySearchAlgorithm extends ArrayAlgorithm {
    private final int target;

    public BinarySearchAlgorithm(int[] sortedInput, int target) {
        super(sortedInput);
        this.target = target;
    }

    @Override
    protected void execute() {
        int lo = 0, hi = array.length - 1;
        while (lo <= hi) {
            recordPartition(lo, hi);
            int mid = (lo + hi) / 2;
            recordCompare(mid, mid);
            if (array[mid] == target) {
                recordFound(mid);
                return;
            } else if (array[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
    }

    @Override
    public String getName() { return "Binary Search"; }

    @Override
    public String getComplexity() { return "O(log n) time, O(1) space"; }

    @Override
    public String getDescription() {
        return "Repeatedly halves a sorted array's search range by comparing the " +
                "middle element to the target.";
    }
}