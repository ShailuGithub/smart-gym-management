package servlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory fixed-window rate limiter, keyed by caller-supplied string (typically the
 * client's IP address). Self-contained on purpose - no external service/dependency, matching
 * this project's "no frameworks" style - and intentionally basic: a deterrent against casual
 * credential-stuffing bots and one attacker hammering from one address, not a WAF.
 *
 * Only meaningful for a single-instance deployment (state is in-process memory) - this app
 * isn't clustered, so that's fine here.
 */
public final class RateLimiter {

    private static final Map<String, RateLimiter> NAMED_LIMITERS = new ConcurrentHashMap<>();

    static {
        // Daemon so it never blocks server shutdown - purely housekeeping, nothing correctness-
        // critical depends on a sweep actually happening before the JVM exits.
        ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-sweep");
            t.setDaemon(true);
            return t;
        });
        sweeper.scheduleAtFixedRate(RateLimiter::sweepAll, 1, 1, TimeUnit.HOURS);
    }

    /** Returns the shared limiter for this name, creating it on first use with the given limits. */
    public static RateLimiter forName(String name, int maxAttempts, long windowMillis) {
        return NAMED_LIMITERS.computeIfAbsent(name, n -> new RateLimiter(maxAttempts, windowMillis));
    }

    private static void sweepAll() {
        for (RateLimiter limiter : NAMED_LIMITERS.values()) {
            limiter.sweep();
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;

    private RateLimiter(int maxAttempts, long windowMillis) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
    }

    /** Returns true if this key still has attempts left in its current window (and counts this call as one). */
    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) ->
                existing == null || now - existing.startMillis > windowMillis ? new Window(now) : existing);
        return window.count.incrementAndGet() <= maxAttempts;
    }

    /** Drops windows that closed more than windowMillis ago, so long uptime doesn't leak memory. */
    private void sweep() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis > windowMillis);
    }

    private static final class Window {
        final long startMillis;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }
}
