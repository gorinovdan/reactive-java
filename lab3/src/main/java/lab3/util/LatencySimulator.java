package lab3.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAccumulator;

/**
 * Busy-work latency emulator used by lab3 benchmarks. It intentionally keeps the CPU busy so that
 * timing measurements capture realistic processing rather than scheduler-induced sleeps.
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

        while ((elapsed = System.nanoTime() - start) < targetNanos) {
            Thread.onSpinWait();
        }

        SINK.accumulate(accumulator);
    }

    private static long toNanosSafely(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        try {
            return duration.toNanos();
        } catch (ArithmeticException overflow) {
            return TimeUnit.SECONDS.toNanos(1);
        }
    }
}
