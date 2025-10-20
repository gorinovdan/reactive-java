package rj.lab1.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAccumulator;

/**
 * Helper for emulating deterministic latency without putting the thread to sleep.
 * <p>
 * The implementation performs calibrated CPU work until the requested wall-clock time
 * has elapsed. That keeps benchmarks closer to realistic processing pipelines where
 * the thread remains busy, and it removes the scheduling noise that {@link Thread#sleep(long)}
 * tends to introduce in microbenchmarks.
 */
public final class LatencySimulator {

    private static final int LOOP_BATCH = 512;
    private static final DoubleAccumulator SINK = new DoubleAccumulator(Double::sum, 0.0d);

    private LatencySimulator() {
    }

    public static void emulateWork(long delayMillis) {
        if (delayMillis <= 0) {
            return;
        }
        emulateWork(Duration.ofMillis(delayMillis));
    }

    public static void emulateWork(Duration duration) {
        if (duration == null) {
            return;
        }
        Duration positiveDuration = duration.isNegative() ? Duration.ZERO : duration;
        if (positiveDuration.isZero()) {
            return;
        }

        long targetNanos = toNanosSafely(positiveDuration);
        if (targetNanos <= 0) {
            return;
        }

        long start = System.nanoTime();
        double accumulator = 0.0d;
        long iterations = 0L;
        long elapsed = 0L;

        while (elapsed < targetNanos) {
            accumulator = Math.fma(accumulator + 1.0d, 1.000_000_119d, (iterations & 0xFF) * 0.000_000_1d);
            iterations++;
            if ((iterations & (LOOP_BATCH - 1)) == 0) {
                elapsed = System.nanoTime() - start;
            }
        }

        // Fine tune the remaining nanoseconds with a short spin.
        while ((elapsed = System.nanoTime() - start) < targetNanos) {
            Thread.onSpinWait();
        }

        // Prevent the JIT from eliminating the busy loop as dead code.
        SINK.accumulate(accumulator);
    }

    private static long toNanosSafely(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        try {
            return duration.toNanos();
        } catch (ArithmeticException overflow) {
            // Cap at 1 second to avoid unbounded busy loops when a huge duration is passed accidentally.
            return TimeUnit.SECONDS.toNanos(1);
        }
    }
}
