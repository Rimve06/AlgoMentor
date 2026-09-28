package com.algomentor.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the real, exact core logic of every algorithm in the app as plain
 * text, so the "Code" panel next to each visualizer shows the actual Java
 * that just ran - not a simplified paraphrase. Kept as one small lookup
 * table (rather than a method on each algorithm class) so it stays purely
 * additive: none of the ten algorithm classes needed to change to support
 * this panel.
 */
public final class AlgorithmCodeSnippets {

    private static final Map<String, String> SNIPPETS = new LinkedHashMap<>();

    static {
        SNIPPETS.put("Bubble Sort", """
                int n = array.length;
                for (int pass = 0; pass < n - 1; pass++) {
                    boolean swapped = false;
                    for (int i = 0; i < n - pass - 1; i++) {
                        if (array[i] > array[i + 1]) {
                            swap(array, i, i + 1);
                            swapped = true;
                        }
                    }
                    if (!swapped) break; // already sorted, stop early
                }""");

        SNIPPETS.put("Selection Sort", """
                int n = array.length;
                for (int i = 0; i < n - 1; i++) {
                    int minIndex = i;
                    for (int j = i + 1; j < n; j++) {
                        if (array[j] < array[minIndex]) minIndex = j;
                    }
                    if (minIndex != i) swap(array, i, minIndex);
                }""");

        SNIPPETS.put("Insertion Sort", """
                int n = array.length;
                for (int i = 1; i < n; i++) {
                    int j = i;
                    while (j > 0 && array[j - 1] > array[j]) {
                        swap(array, j - 1, j);
                        j--;
                    }
                }""");

        SNIPPETS.put("Merge Sort", """
                void mergeSort(int lo, int hi) {
                    if (lo >= hi) return;
                    int mid = (lo + hi) / 2;
                    mergeSort(lo, mid);
                    mergeSort(mid + 1, hi);
                    merge(lo, mid, hi);
                }

                void merge(int lo, int mid, int hi) {
                    int[] left = Arrays.copyOfRange(array, lo, mid + 1);
                    int[] right = Arrays.copyOfRange(array, mid + 1, hi + 1);
                    int i = 0, j = 0, k = lo;
                    while (i < left.length && j < right.length) {
                        array[k++] = (left[i] <= right[j]) ? left[i++] : right[j++];
                    }
                    while (i < left.length) array[k++] = left[i++];
                    while (j < right.length) array[k++] = right[j++];
                }""");

        SNIPPETS.put("Quick Sort", """
                void quickSort(int lo, int hi) {
                    if (lo >= hi) return;
                    int pivotIndex = partition(lo, hi);
                    quickSort(lo, pivotIndex - 1);
                    quickSort(pivotIndex + 1, hi);
                }

                // Lomuto partition scheme - last element is the pivot.
                int partition(int lo, int hi) {
                    int pivot = array[hi];
                    int i = lo - 1;
                    for (int j = lo; j < hi; j++) {
                        if (array[j] < pivot) {
                            i++;
                            swap(array, i, j);
                        }
                    }
                    swap(array, i + 1, hi);
                    return i + 1;
                }""");

        SNIPPETS.put("Heap Sort", """
                void heapSort() {
                    int n = array.length;
                    for (int i = n / 2 - 1; i >= 0; i--) siftDown(i, n);
                    for (int end = n - 1; end > 0; end--) {
                        swap(array, 0, end);
                        siftDown(0, end);
                    }
                }

                void siftDown(int root, int size) {
                    int largest = root;
                    while (true) {
                        int left = 2 * largest + 1, right = 2 * largest + 2, candidate = largest;
                        if (left < size && array[left] > array[candidate]) candidate = left;
                        if (right < size && array[right] > array[candidate]) candidate = right;
                        if (candidate == largest) break;
                        swap(array, largest, candidate);
                        largest = candidate;
                    }
                }""");

        SNIPPETS.put("Binary Search", """
                int lo = 0, hi = array.length - 1;
                while (lo <= hi) {
                    int mid = (lo + hi) / 2;
                    if (array[mid] == target) {
                        return mid; // found it
                    } else if (array[mid] < target) {
                        lo = mid + 1;
                    } else {
                        hi = mid - 1;
                    }
                }
                return -1; // not found""");

        SNIPPETS.put("Breadth-First Search", """
                Set<Integer> visited = new HashSet<>();
                ArrayDeque<Integer> queue = new ArrayDeque<>();

                queue.add(startId);
                visited.add(startId);

                while (!queue.isEmpty()) {
                    int current = queue.poll();
                    visit(current);
                    for (int neighbor : graph.get(current).getNeighbors()) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor); // FIFO -> explores level by level
                        }
                    }
                }""");

        SNIPPETS.put("Depth-First Search", """
                Set<Integer> visited = new HashSet<>();
                ArrayDeque<Integer> stack = new ArrayDeque<>();

                stack.push(startId);

                while (!stack.isEmpty()) {
                    int current = stack.pop();
                    if (visited.contains(current)) continue;
                    visited.add(current);
                    visit(current);
                    for (int neighbor : graph.get(current).getNeighbors()) {
                        if (!visited.contains(neighbor)) {
                            stack.push(neighbor); // LIFO -> dives deep before backtracking
                        }
                    }
                }""");

        SNIPPETS.put("Dijkstra's Algorithm", """
                Map<Integer, Integer> dist = new HashMap<>();
                for (int id : graph.getNodes().keySet()) dist.put(id, Integer.MAX_VALUE);
                dist.put(startId, 0);

                PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));
                pq.add(new int[]{startId, 0});

                while (!pq.isEmpty()) {
                    int[] top = pq.poll();
                    int u = top[0], d = top[1];
                    if (d > dist.get(u)) continue; // stale entry, skip

                    for (int v : graph.get(u).getNeighbors()) {
                        int newDist = d + graph.get(u).getWeight(v);
                        if (newDist < dist.get(v)) {
                            dist.put(v, newDist);
                            pq.add(new int[]{v, newDist}); // relax the edge
                        }
                    }
                }""");
    }

    private AlgorithmCodeSnippets() {}

    public static String get(String algorithmName) {
        return SNIPPETS.getOrDefault(algorithmName, "// Source not available for " + algorithmName);
    }
}
