package com.algomentor.algorithm;

public class HeapSortAlgorithm extends ArrayAlgorithm {

    public HeapSortAlgorithm(int[] input) {
        super(input);
    }

    @Override
    protected void execute() {
        int n = array.length;
        for (int i = n / 2 - 1; i >= 0; i--) {
            siftDown(i, n);
        }
        for (int end = n - 1; end > 0; end--) {
            recordSwap(0, end);
            siftDown(0, end);
        }
    }

    /** Restores the max-heap property for the subtree rooted at `root`, within array[0, size). */
    private void siftDown(int root, int size) {
        int largest = root;
        while (true) {
            int left = 2 * largest + 1;
            int right = 2 * largest + 2;
            int candidate = largest;

            if (left < size) {
                recordCompare(left, candidate);
                if (array[left] > array[candidate]) candidate = left;
            }
            if (right < size) {
                recordCompare(right, candidate);
                if (array[right] > array[candidate]) candidate = right;
            }
            if (candidate == largest) break;
            recordSwap(largest, candidate);
            largest = candidate;
        }
    }

    @Override
    public String getName() { return "Heap Sort"; }

    @Override
    public String getComplexity() { return "O(n log n) time (all cases), O(1) space"; }

    @Override
    public String getDescription() {
        return "Builds a max-heap from the array, then repeatedly moves the largest remaining " +
                "value to the end and re-heapifies. Guaranteed O(n log n) with no extra memory, " +
                "unlike merge sort.";
    }
}
