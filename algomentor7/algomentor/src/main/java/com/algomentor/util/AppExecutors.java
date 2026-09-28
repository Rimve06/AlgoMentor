package com.algomentor.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single shared, bounded thread pool for all background work in the app
 * (network calls, DB access). Demonstrates deliberate thread-pool sizing
 * instead of spawning raw {@code new Thread()} per task or relying on the
 * unbounded {@code Executors.newCachedThreadPool()}.
 *
 * UI updates coming back from this pool are always marshalled back onto the
 * JavaFX Application Thread with {@code Platform.runLater(...)} at the call
 * site - this class only owns the background side.
 */
public final class AppExecutors {
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    private static final ThreadFactory FACTORY = r -> {
        Thread t = new Thread(r, "algomentor-worker-" + THREAD_COUNT.incrementAndGet());
        t.setDaemon(true);
        return t;
    };

    // Fixed pool: 4 threads is enough for a handful of concurrent DB/network
    // calls without letting a runaway task explosion starve the UI thread.
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, FACTORY);

    private AppExecutors() {}

    public static ExecutorService get() {
        return POOL;
    }

    public static void shutdown() {
        POOL.shutdown();
    }
}