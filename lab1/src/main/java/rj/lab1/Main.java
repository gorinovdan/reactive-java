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

    // Тестируем только 5k и 250k
    private static final int[] DATASET_SIZES = { 5_000, 250_000 };

    public static void main(String[] args) {
        // Простой генератор
        SimpleReceiptGenerator simpleGen = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);

        // Сложный генератор (больше товаров, больший разброс цен)
        SimpleReceiptGenerator complexGen = new SimpleReceiptGenerator()
                .withItemRange(10, 50)
                .withPriceRange(1.0, 1_000.0);

        System.out.println("\n=== SIMPLE DATASETS ===\n");
        runBenchmarks("Simple", simpleGen);

        System.out.println("\n=== COMPLEX DATASETS ===\n");
        runBenchmarks("Complex", complexGen);
    }

    private static void runBenchmarks(String label, SimpleReceiptGenerator generator) {
        printBenchHeader(label);

        for (int size : DATASET_SIZES) {
            List<Receipt> receipts = generator.generateMany(size);

            Measurement<ReceiptStatistics> iterative = measure(
                    () -> ReceiptStatisticsIterateCircleAggregator.aggregate(receipts));
            Measurement<ReceiptStatistics> stream = measure(
                    () -> ReceiptStatisticsStreamAggregator.aggregate(receipts));
            Measurement<ReceiptStatistics> customCollector = measure(
                    () -> ReceiptStatisticsStreamCustomAggregator.aggregate(receipts));

            printBenchRow(size, iterative.duration(), stream.duration(), customCollector.duration());
        }

        System.out.println();
    }

    // ---- Formatting helpers ----

    private static void printBenchHeader(String label) {
        System.out.println("Benchmarks (" + label + " dataset, lower is better)");
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

    private static <T> Measurement<T> measure(Supplier<T> supplier) {
        Instant start = Instant.now();
        T result = supplier.get();
        Instant end = Instant.now();
        return new Measurement<>(result, Duration.between(start, end));
    }

    private record Measurement<T>(T result, Duration duration) {
    }
}
