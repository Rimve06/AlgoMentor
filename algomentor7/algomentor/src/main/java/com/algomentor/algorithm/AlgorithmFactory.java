package com.algomentor.algorithm;

import com.algomentor.model.DemoGraph;

/**
 * Simple factory that hides construction details of each algorithm from
 * MainController - the controller asks for an algorithm by name and gets
 * back a Traceable, without needing to know which concrete class or
 * constructor arguments are involved.
 */
public final class AlgorithmFactory {

    public static final String[] NAMES = {
            "Bubble Sort", "Selection Sort", "Insertion Sort",
            "Merge Sort", "Quick Sort", "Heap Sort",
            "Binary Search",
            "Breadth-First Search", "Depth-First Search", "Dijkstra's Algorithm"
    };

    /** Topic grouping, used by the Practice Problems and Compare Algorithms panels. */
    public static final String[] SORTING_NAMES = {
            "Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort", "Heap Sort"
    };
    public static final String[] SEARCH_NAMES = { "Binary Search" };
    public static final String[] GRAPH_NAMES = { "Breadth-First Search", "Depth-First Search", "Dijkstra's Algorithm" };

    private AlgorithmFactory() {}

    public static Traceable createArraySort(String name, int[] data) {
        return switch (name) {
            case "Bubble Sort" -> new BubbleSortAlgorithm(data);
            case "Selection Sort" -> new SelectionSortAlgorithm(data);
            case "Insertion Sort" -> new InsertionSortAlgorithm(data);
            case "Merge Sort" -> new MergeSortAlgorithm(data);
            case "Quick Sort" -> new QuickSortAlgorithm(data);
            case "Heap Sort" -> new HeapSortAlgorithm(data);
            default -> throw new IllegalArgumentException("Not a sort algorithm: " + name);
        };
    }

    public static Traceable createSearch(int[] sortedData, int target) {
        return new BinarySearchAlgorithm(sortedData, target);
    }

    public static Traceable createGraphTraversal(String name, DemoGraph graph, int startId) {
        return switch (name) {
            case "Breadth-First Search" -> new BFSAlgorithm(graph, startId);
            case "Depth-First Search" -> new DFSAlgorithm(graph, startId);
            case "Dijkstra's Algorithm" -> new DijkstraAlgorithm(graph, startId);
            default -> throw new IllegalArgumentException("Not a graph algorithm: " + name);
        };
    }

    public static boolean isGraphAlgorithm(String name) {
        for (String n : GRAPH_NAMES) if (n.equals(name)) return true;
        return false;
    }

    public static boolean isSearchAlgorithm(String name) {
        for (String n : SEARCH_NAMES) if (n.equals(name)) return true;
        return false;
    }

    /** Returns the topic/category name a given algorithm belongs to (for Practice Problems / Compare). */
    public static String categoryOf(String algorithmName) {
        for (String n : SORTING_NAMES) if (n.equals(algorithmName)) return "Sorting";
        for (String n : SEARCH_NAMES) if (n.equals(algorithmName)) return "Binary Search";
        if (algorithmName.equals("Breadth-First Search") || algorithmName.equals("Depth-First Search")) return "Graph Traversal";
        if (algorithmName.equals("Dijkstra's Algorithm")) return "Shortest Path";
        return "General";
    }
}
