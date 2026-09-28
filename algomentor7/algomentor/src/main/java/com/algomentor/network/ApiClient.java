package com.algomentor.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around java.net.http.HttpClient that performs GET/POST
 * requests to a JSON API asynchronously, off the JavaFX Application Thread.
 *
 * Concurrency note: the app-wide bounded thread pool
 * (see {@link com.algomentor.util.AppExecutors}) backs both this client and
 * the ArrayAlgorithm playback engine, so network work and animation work
 * never fight over one shared unbounded pool.
 */
public class ApiClient {
    /** Network-level failures (dropped connection, "EOF reached while reading") are retried this many times. */
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final ExecutorService executor;

    public ApiClient(ExecutorService executor) {
        this.executor = executor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                // HTTP/1.1 on purpose: the JDK's HTTP/2 client often dies with "EOF reached while
                // reading" when a server/proxy silently drops a pooled connection.
                .version(HttpClient.Version.HTTP_1_1)
                .executor(executor)
                .build();
    }

    /** Fetches a URL and parses the response body as JSON, off the FX thread. */
    public CompletableFuture<JsonValue> getJson(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new RuntimeException("HTTP " + response.statusCode() + " from " + url);
                    }
                    return JsonParser.parse(response.body());
                }, executor);
    }

    /**
     * POSTs a JSON body and parses the JSON response, off the FX thread.
     * If the connection drops mid-request (an IOException such as "EOF reached while
     * reading"), the request is retried automatically up to MAX_ATTEMPTS times on a fresh
     * connection. HTTP error statuses (401, 404, 429...) are NOT retried - they are real answers.
     */
    public CompletableFuture<JsonValue> postJson(String url, String jsonBody, String... headers) {
        CompletableFuture<JsonValue> result = new CompletableFuture<>();
        attemptPost(url, jsonBody, headers, 1, result);
        return result;
    }

    private void attemptPost(String url, String jsonBody, String[] headers, int attempt,
                             CompletableFuture<JsonValue> result) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(90))   // long answers (complexity analysis) need more than 20s
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (headers.length > 0) builder.headers(headers);

        httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
                    }
                    return JsonParser.parse(response.body());
                }, executor)
                .whenComplete((value, error) -> {
                    if (error == null) {
                        result.complete(value);
                        return;
                    }
                    Throwable root = unwrap(error);
                    if (root instanceof IOException && attempt < MAX_ATTEMPTS) {
                        Executor delayed = CompletableFuture.delayedExecutor(
                                700L * attempt, TimeUnit.MILLISECONDS, executor);
                        delayed.execute(() -> attemptPost(url, jsonBody, headers, attempt + 1, result));
                    } else {
                        result.completeExceptionally(root);
                    }
                });
    }

    private static Throwable unwrap(Throwable t) {
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
