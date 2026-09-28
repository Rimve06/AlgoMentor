package com.algomentor.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static teaching content for every algorithm in the app:
 *  1. the "Raw Algorithm" panel text (plain-English steps + an imagination
 *     tip + what to watch while it runs), and
 *  2. hard, verified complexity facts used for the comparison table and to
 *     ground the AI analysis prompt (so the AI never has to guess Big-O).
 * Kept offline and deterministic on purpose: it works even with no API key.
 */
public final class AlgorithmGuide {

    /** Verified facts for one algorithm, used by the comparison table and the AI prompt. */
    public record Facts(String name, String best, String average, String worst,
                        String space, String trait, String useWhen) {
        public String traitAndUse() { return trait + ". Best when: " + useWhen; }
    }

    private record Guide(String steps, String imagine, String watch) {}

    private static final Map<String, Guide> GUIDES = new LinkedHashMap<>();
    private static final Map<String, Facts> FACTS = new LinkedHashMap<>();

    static {
        // ------------------------------------------------------------ guides
        GUIDES.put("Bubble Sort", new Guide("""
                1. Start at the left end of the list.
                2. Compare the two neighbours side by side.
                3. If the left one is bigger, swap them.
                4. Move one step right and repeat until the end of the list.
                5. The biggest value has now "bubbled" to the far right - it is locked in place.
                6. Repeat from the start, one position shorter each time.
                7. If a full pass makes zero swaps, the list is sorted - stop early.""",
                "Picture bubbles in a fizzy drink: the biggest bubble rises to the top first, then the next biggest, and so on. Each pass floats one more big value into its final seat at the right.",
                "Watch the right side grow into a finished, sorted block after every pass. If the data is nearly sorted, notice how few swaps happen - that is why the early exit matters."));

        GUIDES.put("Selection Sort", new Guide("""
                1. Look at the whole unsorted part of the list.
                2. Scan it and remember where the smallest value is.
                3. Swap that smallest value into the first unsorted position.
                4. Now the sorted part is one longer; the unsorted part is one shorter.
                5. Repeat until only one value is left unsorted.""",
                "Imagine picking the shortest person from a crowd and lining them up first, then picking the shortest of the remaining crowd for second place, and so on.",
                "Watch how many comparisons it does even when the data is already sorted (it always scans everything), but how few swaps it makes - at most one per pass."));

        GUIDES.put("Insertion Sort", new Guide("""
                1. Treat the first value as a tiny sorted group.
                2. Take the next value from the unsorted part.
                3. Slide it left past every bigger value until it fits.
                4. The sorted group is now one bigger.
                5. Repeat until every value has been inserted.""",
                "Think of sorting playing cards in your hand: you pick up one card at a time and slide it into the right spot among the cards you already hold.",
                "Watch a value travel left only as far as it needs. On nearly sorted data it barely moves - that is why Insertion Sort is fast on almost-sorted lists."));

        GUIDES.put("Merge Sort", new Guide("""
                1. If the list has 0 or 1 items, it is already sorted - stop.
                2. Split the list into a left half and a right half.
                3. Sort the left half (same steps, recursively).
                4. Sort the right half (same steps, recursively).
                5. Merge: compare the front items of both halves, always take the smaller, repeat until both halves are used up.""",
                "Imagine two already-sorted piles of exam papers. You look at the top paper of each pile and always take the lower mark first - you build one perfectly sorted pile without ever going backwards.",
                "Watch the list get cut into smaller and smaller pieces, then zip back together in order. The work per level is the same, and there are only about log n levels."));

        GUIDES.put("Quick Sort", new Guide("""
                1. Pick one value as the pivot.
                2. Walk through the list: move values smaller than the pivot to its left, larger ones to its right.
                3. The pivot is now in its final sorted position.
                4. Sort the left part (same steps, recursively).
                5. Sort the right part (same steps, recursively).
                6. Parts of size 0 or 1 are already sorted.""",
                "Picture a teacher asking everyone shorter than a chosen student to stand on the left and everyone taller on the right. That student is now in the right place - then each group repeats the trick.",
                "Watch where the pivot lands. A pivot near the middle splits the work evenly (fast); a pivot that is always the smallest or biggest gives lopsided splits (the slow O(n^2) worst case)."));

        GUIDES.put("Heap Sort", new Guide("""
                1. Rearrange the list into a max-heap (every parent is bigger than its children).
                2. The biggest value is now at the front - swap it with the last position.
                3. Shrink the heap by one (the last value is now locked in place).
                4. Sift the new front value down until the heap rule holds again.
                5. Repeat until the heap is empty.""",
                "Imagine a tournament bracket where the champion always sits at the top. Take the champion out, let the others replay a quick round to crown a new one, and repeat - champions come out biggest to smallest.",
                "Watch the sorted block grow from the right while the heap on the left keeps re-organising. Each sift-down is at most about log n moves."));

        GUIDES.put("Binary Search", new Guide("""
                1. Requires a SORTED list. Set low = first index, high = last index.
                2. Look at the middle value.
                3. If it equals the target - found it, done.
                4. If the target is smaller, throw away the right half (high = mid - 1).
                5. If the target is bigger, throw away the left half (low = mid + 1).
                6. Repeat until found, or low passes high (not in the list).""",
                "It is the 'guess the number' game: you guess 50 out of 100, hear 'higher', and instantly rule out half of all numbers. Each guess halves what is left.",
                "Watch the search window shrink by half each step. 1,000,000 items need only about 20 checks - compare that with 1,000,000 checks for scanning one by one."));

        GUIDES.put("Breadth-First Search", new Guide("""
                1. Put the start node in a queue and mark it visited.
                2. Take the node at the front of the queue.
                3. Visit all its unvisited neighbours, mark them, and add them to the back of the queue.
                4. Repeat from step 2 until the queue is empty.""",
                "Imagine dropping a stone in a pond: ripples spread outward one ring at a time. BFS explores everything 1 step away, then everything 2 steps away, and so on.",
                "Watch the yellow frontier spread outward in rings. Because it goes level by level, the first time it reaches a node is via the fewest hops (in an unweighted graph)."));

        GUIDES.put("Depth-First Search", new Guide("""
                1. Start at the start node and mark it visited.
                2. Pick an unvisited neighbour and go there immediately.
                3. Keep going deeper the same way.
                4. At a dead end, backtrack to the last node that still has an unvisited neighbour.
                5. Stop when there is nothing left to backtrack to.""",
                "Imagine exploring a maze by always taking the next unexplored corridor as far as it goes, and only turning back when you hit a wall. A ball of string helps you backtrack.",
                "Watch it plunge deep along one branch before ever touching the others. The order of visits is very different from BFS on the same graph."));

        GUIDES.put("Dijkstra's Algorithm", new Guide("""
                1. Give the start node distance 0 and every other node distance infinity.
                2. Pick the unsettled node with the smallest known distance.
                3. Mark it settled - its distance is now final.
                4. For each neighbour, check: is (my distance + edge weight) smaller than its current distance? If so, update it.
                5. Repeat until all reachable nodes are settled.""",
                "Think of a GPS finding the fastest route: it always extends the closest place it has found so far, and updates the travel time of the roads leading out of it.",
                "Watch the distance labels shrink when a shorter route is discovered. Once a node is settled its number never changes again - this only works with non-negative edge weights."));

        // ------------------------------------------------------------- facts
        FACTS.put("Bubble Sort", new Facts("Bubble Sort", "O(n)", "O(n\u00b2)", "O(n\u00b2)", "O(1)",
                "Stable, in-place, stops early if already sorted", "teaching, tiny or nearly sorted data"));
        FACTS.put("Selection Sort", new Facts("Selection Sort", "O(n\u00b2)", "O(n\u00b2)", "O(n\u00b2)", "O(1)",
                "Unstable, in-place, at most n-1 swaps", "writes/swaps are expensive"));
        FACTS.put("Insertion Sort", new Facts("Insertion Sort", "O(n)", "O(n\u00b2)", "O(n\u00b2)", "O(1)",
                "Stable, in-place, adaptive", "data is small or nearly sorted"));
        FACTS.put("Merge Sort", new Facts("Merge Sort", "O(n log n)", "O(n log n)", "O(n log n)", "O(n)",
                "Stable, guaranteed speed, needs extra memory", "you need predictable speed on large data"));
        FACTS.put("Quick Sort", new Facts("Quick Sort", "O(n log n)", "O(n log n)", "O(n\u00b2)", "O(log n)",
                "Unstable, in-place, very fast in practice", "general-purpose in-memory sorting"));
        FACTS.put("Heap Sort", new Facts("Heap Sort", "O(n log n)", "O(n log n)", "O(n log n)", "O(1)",
                "Unstable, in-place, guaranteed speed", "memory is tight but worst-case must stay fast"));
        FACTS.put("Binary Search", new Facts("Binary Search", "O(1)", "O(log n)", "O(log n)", "O(1)",
                "Needs sorted data, halves the search every step", "looking up values in a sorted array"));
        FACTS.put("Linear Search", new Facts("Linear Search", "O(1)", "O(n)", "O(n)", "O(1)",
                "Works on unsorted data, checks one by one", "data is unsorted or very small"));
        FACTS.put("Breadth-First Search", new Facts("Breadth-First Search", "O(V + E)", "O(V + E)", "O(V + E)", "O(V)",
                "Explores level by level, finds fewest-hop paths", "shortest path in an unweighted graph"));
        FACTS.put("Depth-First Search", new Facts("Depth-First Search", "O(V + E)", "O(V + E)", "O(V + E)", "O(V)",
                "Dives deep then backtracks, simple recursion", "cycles, connectivity, mazes, topological order"));
        FACTS.put("Dijkstra's Algorithm", new Facts("Dijkstra's Algorithm", "O((V+E) log V)", "O((V+E) log V)", "O((V+E) log V)", "O(V)",
                "Weighted shortest paths, no negative edges", "route finding on weighted graphs"));
    }

    private AlgorithmGuide() {}

    /** Text for the "Raw Algorithm & Tips" panel. */
    public static String rawAlgorithm(String name) {
        Guide g = GUIDES.get(name);
        if (g == null) return "No guide available for this algorithm yet.";
        return "HOW IT WORKS (plain steps)\n" + g.steps() + "\n\n"
                + "PICTURE IT\n" + g.imagine() + "\n\n"
                + "WATCH FOR\n" + g.watch();
    }

    /** Algorithms compared for a given category (Binary Search gets Linear Search as a baseline). */
    public static List<Facts> comparisonSet(String category) {
        List<Facts> out = new ArrayList<>();
        switch (category) {
            case "Sorting" -> {
                for (String n : new String[]{"Bubble Sort", "Selection Sort", "Insertion Sort",
                        "Merge Sort", "Quick Sort", "Heap Sort"}) out.add(FACTS.get(n));
            }
            case "Binary Search" -> {
                out.add(FACTS.get("Binary Search"));
                out.add(FACTS.get("Linear Search"));
            }
            case "Graph Traversal", "Shortest Path" -> {
                for (String n : new String[]{"Breadth-First Search", "Depth-First Search",
                        "Dijkstra's Algorithm"}) out.add(FACTS.get(n));
            }
            default -> { }
        }
        return out;
    }
}
