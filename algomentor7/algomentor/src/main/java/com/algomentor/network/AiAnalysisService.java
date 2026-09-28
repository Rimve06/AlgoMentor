package com.algomentor.network;

import com.algomentor.util.AppConfig;
import com.algomentor.util.AppExecutors;

import java.util.concurrent.CompletableFuture;

/**
 * Talks to the Anthropic Messages API to get a short plain-English tip about
 * an algorithm/complexity, demonstrating the "networking + JSON parsing"
 * requirement end to end: build request JSON by hand, POST it, parse the
 * response with {@link JsonParser}, pull out the text field.
 *
 * If no API key is configured (ANTHROPIC_API_KEY in algomentor.properties or
 * the environment), falls back to a small built-in set of canned tips so the
 * app still runs fully without credentials. If a key IS configured but the
 * call fails (bad key, no quota, network error), that real failure reason is
 * shown instead of silently returning a canned tip - a wrong/expired key
 * should never look identical to "no key configured".
 */
public class AiAnalysisService {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";

    private final ApiClient apiClient;

    public AiAnalysisService() {
        this.apiClient = new ApiClient(AppExecutors.get());
    }

    public CompletableFuture<String> getTip(String algorithmName, String complexity) {
        String apiKey = AppConfig.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture(fallbackTip(algorithmName, complexity));
        }
        if (!apiKey.startsWith("sk-ant-")) {
            return CompletableFuture.completedFuture(
                    "ANTHROPIC_API_KEY doesn't look like a real Anthropic key (those start with "
                    + "\"sk-ant-\"). If it starts with \"sk-proj-\" instead, that's actually an OpenAI "
                    + "key - move it to OPENAI_API_KEY, and put a real Anthropic key here (or leave "
                    + "this one blank, it's optional).");
        }

        String prompt = "In two sentences, give a student a memorable tip for understanding "
                + algorithmName + " (" + complexity + "). Plain text only, no markdown.";

        JsonValue body = JsonValue.newObject();
        body.put("model", JsonValue.ofString(MODEL));
        body.put("max_tokens", JsonValue.ofNumber(200));
        JsonValue message = JsonValue.newObject();
        message.put("role", JsonValue.ofString("user"));
        message.put("content", JsonValue.ofString(prompt));
        JsonValue messages = JsonValue.newArray();
        messages.asArray().add(message);
        body.put("messages", messages);

        return apiClient.postJson(API_URL, body.toJson(),
                        "x-api-key", apiKey,
                        "anthropic-version", "2023-06-01")
                .thenApply(this::extractText)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    return "Could not reach Anthropic (" + cause.getMessage() + "). Check "
                            + "ANTHROPIC_API_KEY in algomentor.properties.";
                });
    }

    private String extractText(JsonValue response) {
        JsonValue content = response.get("content");
        if (content != null && content.isArray() && !content.asArray().isEmpty()) {
            JsonValue first = content.asArray().get(0);
            JsonValue text = first.get("text");
            if (text != null) return text.asString();
        }
        throw new RuntimeException("Unexpected response shape");
    }

    private String fallbackTip(String algorithmName, String complexity) {
        return switch (algorithmName) {
            case "Bubble Sort" -> "Think of bubble sort as bubbles rising: each pass " +
                    "pushes the largest remaining value to its final spot at the end.";
            case "Merge Sort" -> "Merge sort trusts recursion completely: sort two halves " +
                    "you assume are already correct, then just zipper them together.";
            case "Binary Search" -> "Binary search only works because the array is sorted - " +
                    "each comparison throws away half of what's left to check.";
            case "Breadth-First Search" -> "BFS spreads outward ring by ring using a queue, " +
                    "which is exactly why it finds the shortest path in an unweighted graph.";
            case "Depth-First Search" -> "DFS commits to one path as far as it can go using a " +
                    "stack, then backtracks - good for exploring, not for shortest paths.";
            default -> "Complexity: " + complexity + ". (Offline mode - set ANTHROPIC_API_KEY for live AI tips.)";
        };
    }
}