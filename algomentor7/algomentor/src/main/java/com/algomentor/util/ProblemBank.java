package com.algomentor.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small curated bank of REAL, verified LeetCode problems for each topic
 * AlgoMentor covers. These are genuine, well-known problems with correct
 * links - unlike an AI-generated link (which can be plausible-looking but
 * wrong/broken), a hand-curated bank guarantees the student lands on a real
 * problem when they click through.
 */
public final class ProblemBank {

    public record ProblemLink(String title, String url, String difficulty) {}

    private static final Map<String, List<ProblemLink>> BANK = new LinkedHashMap<>();

    static {
        BANK.put("Sorting", List.of(
                new ProblemLink("Sort an Array", "https://leetcode.com/problems/sort-an-array/", "Medium"),
                new ProblemLink("Sort Colors", "https://leetcode.com/problems/sort-colors/", "Medium"),
                new ProblemLink("Merge Sorted Array", "https://leetcode.com/problems/merge-sorted-array/", "Easy"),
                new ProblemLink("Kth Largest Element in an Array", "https://leetcode.com/problems/kth-largest-element-in-an-array/", "Medium")
        ));
        BANK.put("Binary Search", List.of(
                new ProblemLink("Binary Search", "https://leetcode.com/problems/binary-search/", "Easy"),
                new ProblemLink("Search in Rotated Sorted Array", "https://leetcode.com/problems/search-in-rotated-sorted-array/", "Medium"),
                new ProblemLink("Find First and Last Position of Element in Sorted Array", "https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/", "Medium")
        ));
        BANK.put("Graph Traversal", List.of(
                new ProblemLink("Number of Islands", "https://leetcode.com/problems/number-of-islands/", "Medium"),
                new ProblemLink("Clone Graph", "https://leetcode.com/problems/clone-graph/", "Medium"),
                new ProblemLink("Rotting Oranges", "https://leetcode.com/problems/rotting-oranges/", "Medium")
        ));
        BANK.put("Shortest Path", List.of(
                new ProblemLink("Network Delay Time", "https://leetcode.com/problems/network-delay-time/", "Medium"),
                new ProblemLink("Path With Minimum Effort", "https://leetcode.com/problems/path-with-minimum-effort/", "Medium"),
                new ProblemLink("Cheapest Flights Within K Stops", "https://leetcode.com/problems/cheapest-flights-within-k-stops/", "Medium")
        ));
    }

    private ProblemBank() {}

    public static List<String> topics() {
        return List.copyOf(BANK.keySet());
    }

    public static List<ProblemLink> problemsFor(String topic) {
        return BANK.getOrDefault(topic, List.of());
    }
}
