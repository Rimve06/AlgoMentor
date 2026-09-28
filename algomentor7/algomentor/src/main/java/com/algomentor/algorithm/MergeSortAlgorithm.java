package com.algomentor.algorithm;

public class MergeSortAlgorithm extends ArrayAlgorithm {

    public MergeSortAlgorithm(int[] input) {
        super(input);
    }

    @Override
    protected void execute() {
        mergeSort(0, array.length - 1);
    }

    private void mergeSort(int lo, int hi) {
        if (lo >= hi) return;
        recordPartition(lo, hi);
        int mid = (lo + hi) / 2;
        mergeSort(lo, mid);
        mergeSort(mid + 1, hi);
        merge(lo, mid, hi);
    }

    private void merge(int lo, int mid, int hi) {
        int[] left = new int[mid - lo + 1];
        int[] right = new int[hi - mid];
        System.arraycopy(array, lo, left, 0, left.length);
        System.arraycopy(array, mid + 1, right, 0, right.length);

        int i = 0, j = 0, k = lo;
        while (i < left.length && j < right.length) {
            recordCompare(lo + i, mid + 1 + j);
            if (left[i] <= right[j]) {
                recordOverwrite(k++, left[i++]);
            } else {
                recordOverwrite(k++, right[j++]);
            }
        }
        while (i < left.length) recordOverwrite(k++, left[i++]);
        while (j < right.length) recordOverwrite(k++, right[j++]);
    }

    @Override
    public String getName() { return "Merge Sort"; }

    @Override
    public String getComplexity() { return "O(n log n) time, O(n) space"; }

    @Override
    public String getDescription() {
        return "Recursively splits the array in half, sorts each half, then merges " +
                "the two sorted halves back together.";
    }
}