package rj.lab2.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import rj.lab2.statistics.ReceiptStatistics;

public final class ReportGenerator {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);
    private static final DateTimeFormatter HUMAN_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String REPORTS_DIR = "target/reports";

    private ReportGenerator() {
    }

    public static Path writeReport(List<DatasetReportRow> rows) {
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Report requires at least one dataset row");
        }

        Path reportsDir = Path.of(REPORTS_DIR);
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create reports directory: " + reportsDir, e);
        }

        Path reportPath = reportsDir.resolve("lab2-report-" + FILE_TIMESTAMP.format(LocalDateTime.now()) + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8)) {
            writeHeader(writer);
            writeDatasetSections(writer, rows);
            writeSummary(writer, rows);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write report to " + reportPath, e);
        }

        return reportPath;
    }

    private static void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("Lab 2 Aggregation Report");
        writer.newLine();
        writer.write("Generated at: " + HUMAN_TIMESTAMP.format(LocalDateTime.now()));
        writer.newLine();
        writer.write("Tasks covered:");
        writer.newLine();
        writer.write("  1) Artificial delay toggle for item name retrieval");
        writer.newLine();
        writer.write("  2) Parallel aggregation with thread-safe accumulators");
        writer.newLine();
        writer.write("  3) Custom Spliterator for tuned parallel execution");
        writer.newLine();
        writer.write("  4) Benchmarking via JMH (see shaded jar output)");
        writer.newLine();
        writer.write("============================================================");
        writer.newLine();
        writer.newLine();
    }

    private static void writeDatasetSections(BufferedWriter writer, List<DatasetReportRow> rows) throws IOException {
        for (DatasetReportRow row : rows) {
            writer.write(String.format(Locale.ROOT, "Dataset %,d receipts%n", row.datasetSize()));
            writer.write("------------------------------------------------------------");
            writer.newLine();

            writeAggregatorBlock(writer, "Iterative loop", row.iterativeDuration(), row.iterativeStats());
            writeAggregatorBlock(writer, "Parallel stream", row.parallelDuration(), row.parallelStats());
            writeAggregatorBlock(writer, "Custom collector (parallel)", row.customDuration(), row.customStats());

            writer.write("Best performer: " + determineBest(row));
            writer.newLine();
            writer.newLine();
        }
    }

    private static void writeSummary(BufferedWriter writer, List<DatasetReportRow> rows) throws IOException {
        DatasetReportRow first = rows.get(0);
        DatasetReportRow last = rows.get(rows.size() - 1);

        writer.write("Summary ratios (complex/simple datasets):");
        writer.newLine();
        writer.write(String.format(Locale.ROOT, "  Iterative: %s -> %s (%.2fx)%n",
                fmt(first.iterativeDuration()), fmt(last.iterativeDuration()),
                ratio(first.iterativeDuration(), last.iterativeDuration())));
        writer.write(String.format(Locale.ROOT, "  Parallel stream: %s -> %s (%.2fx)%n",
                fmt(first.parallelDuration()), fmt(last.parallelDuration()),
                ratio(first.parallelDuration(), last.parallelDuration())));
        writer.write(String.format(Locale.ROOT, "  Custom collector: %s -> %s (%.2fx)%n",
                fmt(first.customDuration()), fmt(last.customDuration()),
                ratio(first.customDuration(), last.customDuration())));
        writer.newLine();

        writer.write("Note: JMH benchmarks provide detailed scaling analysis, including artificial delays.");
        writer.newLine();
    }

    private static void writeAggregatorBlock(
            BufferedWriter writer,
            String label,
            Duration duration,
            ReceiptStatistics stats) throws IOException {

        writer.write(String.format(Locale.ROOT, "  %s:%n", label));
        writer.write(String.format(Locale.ROOT, "    Duration: %s%n", fmt(duration)));
        writer.write("    Metrics:");
        writer.newLine();
        indentStatistics(writer, stats.toString(), "      ");
    }

    private static void indentStatistics(BufferedWriter writer, String text, String indent) throws IOException {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            writer.write(indent);
            writer.write(line);
            writer.newLine();
        }
    }

    private static String determineBest(DatasetReportRow row) {
        Duration bestDuration = row.iterativeDuration();
        String bestLabel = "Iterative loop";

        if (row.parallelDuration().compareTo(bestDuration) < 0) {
            bestDuration = row.parallelDuration();
            bestLabel = "Parallel stream";
        }
        if (row.customDuration().compareTo(bestDuration) < 0) {
            bestLabel = "Custom collector (parallel)";
        }

        return bestLabel;
    }

    private static String fmt(Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1_000) {
            return millis + " ms";
        }
        return String.format(Locale.ROOT, "%.3f s", millis / 1_000.0);
    }

    private static double ratio(Duration simple, Duration complex) {
        long base = Math.max(1L, simple.toNanos());
        long target = Math.max(1L, complex.toNanos());
        return target / (double) base;
    }

    public record DatasetReportRow(
            int datasetSize,
            Duration iterativeDuration,
            ReceiptStatistics iterativeStats,
            Duration parallelDuration,
            ReceiptStatistics parallelStats,
            Duration customDuration,
            ReceiptStatistics customStats) {
    }
}
