package rj.lab1;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.report.ReportGenerator;
import rj.lab1.report.ReportGenerator.DatasetReportRow;
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

        List<DatasetReportRow> reportRows = new ArrayList<>();

        // 1) Сначала готовим первый набор и печатаем Sample statistics
        int firstSize = DATASET_SIZES[0];
        List<Receipt> firstReceipts = generator.generateMany(firstSize);

        Measurement<ReceiptStatistics> firstIter = measure(
                () -> ReceiptStatisticsIterateCircleAggregator.aggregate(firstReceipts));
        Measurement<ReceiptStatistics> firstStream = measure(
                () -> ReceiptStatisticsStreamAggregator.aggregate(firstReceipts));
        Measurement<ReceiptStatistics> firstCustom = measure(
                () -> ReceiptStatisticsStreamCustomAggregator.aggregate(firstReceipts));

        reportRows.add(toRow(firstSize, firstIter, firstStream, firstCustom));

        printSampleStats("Sample statistics (stream, simple dataset)", firstStream.result(), 10);
        System.out.println();

        // подготавливаем переменные для сравнения простого и сложного набора
        int complexSize = firstSize;
        Measurement<ReceiptStatistics> complexIter = firstIter;
        Measurement<ReceiptStatistics> complexStream = firstStream;
        Measurement<ReceiptStatistics> complexCustom = firstCustom;

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

            reportRows.add(toRow(size, iterative, stream, customCollector));

            printBenchRow(size, iterative.duration(), stream.duration(), customCollector.duration());

            complexSize = size;
            complexIter = iterative;
            complexStream = stream;
            complexCustom = customCollector;
        }

        System.out.println();
        printSampleStats("Sample statistics (stream, complex dataset)", complexStream.result(), 10);
        System.out.println();
        printComplexityComparison(firstSize, complexSize,
                firstIter, firstStream, firstCustom,
                complexIter, complexStream, complexCustom);

        Path reportPath = ReportGenerator.writeReport(reportRows);
        System.out.println();
        System.out.printf("Detailed report saved to %s%n", reportPath.toAbsolutePath());
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

    private static void printComplexityComparison(
            int simpleSize,
            int complexSize,
            Measurement<ReceiptStatistics> simpleIter,
            Measurement<ReceiptStatistics> simpleStream,
            Measurement<ReceiptStatistics> simpleCustom,
            Measurement<ReceiptStatistics> complexIter,
            Measurement<ReceiptStatistics> complexStream,
            Measurement<ReceiptStatistics> complexCustom) {

        String simpleLabel = String.format("Simple (%s)", fmtSize(simpleSize));
        String complexLabel = String.format("Complex (%s)", fmtSize(complexSize));

        System.out.println("Complexity comparison (simple vs complex datasets)");
        System.out.println("─".repeat(78));
        System.out.printf("%-20s │ %-18s │ %-18s │ %-18s%n",
                "Aggregator", simpleLabel, complexLabel, "Ratio");
        System.out.println("─".repeat(78));
        printComparisonRow("Iterative", simpleIter.duration(), complexIter.duration());
        printComparisonRow("Stream", simpleStream.duration(), complexStream.duration());
        printComparisonRow("Custom collector", simpleCustom.duration(), complexCustom.duration());
    }

    private static void printComparisonRow(String label, Duration simple, Duration complex) {
        double ratio = computeRatio(simple, complex);
        System.out.printf("%-20s │ %-18s │ %-18s │ %-18s%n",
                label,
                fmt(simple),
                fmt(complex),
                String.format("%.2fx", ratio));
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

    private static String fmtSize(int size) {
        return String.format("%,d", size);
    }

    private static double computeRatio(Duration simple, Duration complex) {
        long simpleNanos = Math.max(1L, simple.toNanos());
        long complexNanos = Math.max(0L, complex.toNanos());
        return complexNanos / (double) simpleNanos;
    }

    private static <T> Measurement<T> measure(Supplier<T> supplier) {
        Instant start = Instant.now();
        T result = supplier.get();
        Instant end = Instant.now();
        return new Measurement<>(result, Duration.between(start, end));
    }

    private static DatasetReportRow toRow(
            int datasetSize,
            Measurement<ReceiptStatistics> iterative,
            Measurement<ReceiptStatistics> parallel,
            Measurement<ReceiptStatistics> custom) {

        return new DatasetReportRow(
                datasetSize,
                iterative.duration(),
                iterative.result(),
                parallel.duration(),
                parallel.result(),
                custom.duration(),
                custom.result());
    }

    private record Measurement<T>(T result, Duration duration) {
    }
}
