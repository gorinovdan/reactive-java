package rj.lab1;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatistics;
import rj.lab1.statistics.ReceiptStatisticsIterateCircleAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamCustomAggregator;

public class Main {

    private static final int[] DATASET_SIZES = { 5_000, 50_000, 250_000 };

    public static void main(String[] args) {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);

        // 1) Сначала готовим первый набор и печатаем Sample statistics
        int firstSize = DATASET_SIZES[0];
        List<Receipt> firstReceipts = generator.generateMany(firstSize);

        Measurement<ReceiptStatistics> firstIter = measure(
                () -> ReceiptStatisticsIterateCircleAggregator.aggregate(firstReceipts));
        Measurement<ReceiptStatistics> firstStream = measure(
                () -> ReceiptStatisticsStreamAggregator.aggregate(firstReceipts));
        Measurement<ReceiptStatistics> firstCustom = measure(
                () -> ReceiptStatisticsStreamCustomAggregator.aggregate(firstReceipts));

        printSampleStats("Sample statistics (iterative)", firstIter.result(), 10);
        System.out.println();

        // 2) Затем печатаем Benchmarks-таблицу и сразу первую строку
        printBenchHeader();
        printBenchRow(firstSize, firstIter.duration(), firstStream.duration(), firstCustom.duration());

        // 3) Остальные размеры — как обычно
        for (int i = 1; i < DATASET_SIZES.length; i++) {
            int size = DATASET_SIZES[i];
            List<Receipt> receipts = generator.generateMany(size);

            Measurement<ReceiptStatistics> iterative = measure(
                    () -> ReceiptStatisticsIterateCircleAggregator.aggregate(receipts));
            Measurement<ReceiptStatistics> stream = measure(
                    () -> ReceiptStatisticsStreamAggregator.aggregate(receipts));
            Measurement<ReceiptStatistics> customCollector = measure(
                    () -> ReceiptStatisticsStreamCustomAggregator.aggregate(receipts));

            printBenchRow(size, iterative.duration(), stream.duration(), customCollector.duration());
        }
    }

    // ---- Formatting helpers ----

    private static void printBenchHeader() {
        System.out.println("Benchmarks (lower is better)");
        System.out.println("─".repeat(78));
        System.out.printf("%-12s │ %-18s │ %-18s │ %-18s%n",
                "Receipts", "Iterative", "Stream", "Custom collector");
        System.out.println("─".repeat(78));
    }

    private static void printBenchRow(int size, Duration dIter, Duration dStream, Duration dCustom) {
        long msIter = dIter.toMillis();
        long msStream = dStream.toMillis();
        long msCustom = dCustom.toMillis();
        long best = Math.min(msIter, Math.min(msStream, msCustom));

        System.out.printf("%,12d │ %-18s │ %-18s │ %-18s%n",
                size,
                tagBest(fmt(dIter), msIter == best),
                tagBest(fmt(dStream), msStream == best),
                tagBest(fmt(dCustom), msCustom == best));
    }

    private static String tagBest(String text, boolean isBest) {
        return isBest ? text + " ★" : text;
    }

    private static String fmt(Duration d) {
        long ms = d.toMillis();
        return (ms < 1_000) ? ms + " ms" : String.format("%.3f s", ms / 1000.0);
    }

    private static void printSampleStats(String title, Object stats, int maxLines) {
        System.out.println(title);
        String[] lines = String.valueOf(stats).split("\\R");
        int shown = Math.min(lines.length, maxLines);
        for (int i = 0; i < shown; i++) {
            System.out.println("  • " + lines[i]);
        }
        if (lines.length > shown) {
            System.out.println("  … +" + (lines.length - shown) + " more");
        }
    }

    private static <T> Measurement<T> measure(Supplier<T> supplier) {
        Instant start = Instant.now();
        T result = supplier.get();
        Instant end = Instant.now();
        return new Measurement<>(result, Duration.between(start, end));
    }

    private record Measurement<T>(T result, Duration duration) {
    }
}
