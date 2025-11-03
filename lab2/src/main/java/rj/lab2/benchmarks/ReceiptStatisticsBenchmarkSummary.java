package rj.lab2.benchmarks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Utility launcher that executes {@link ReceiptStatisticsBenchmark} via the JMH API,
 * collects the raw results, and emits pre-processed CSV summaries that can be used
 * for reporting and plotting.
 *
 * <p>The class writes two artefacts under {@code target/reports}:
 * <ul>
 *     <li>{@code lab2-benchmark-summary.csv} &mdash; flattened table with mean execution
 *         time and 95% confidence interval for each dataset size, delay scenario, and
 *         aggregation strategy (sequential stream, parallel stream, custom spliterator).</li>
 *     <li>{@code lab2-benchmark-crossover.csv} &mdash; estimate of the dataset size where
 *         sequential and parallel stream pipelines reach comparable performance for each
 *         delay profile.</li>
 * </ul>
 *
 * <p>Warmup, measurement, and fork counts can be overridden via system properties:
 * {@code lab2.jmh.warmups}, {@code lab2.jmh.measurements}, {@code lab2.jmh.forks}.
 */
public final class ReceiptStatisticsBenchmarkSummary {

    private static final Path REPORT_DIR = Path.of("target", "reports");
    private static final String SUMMARY_FILE = "lab2-benchmark-summary.csv";
    private static final String CROSSOVER_FILE = "lab2-benchmark-crossover.csv";

    private ReceiptStatisticsBenchmarkSummary() {
    }

    public static void main(String[] args) throws RunnerException {
        Options options = buildOptions();
        Collection<RunResult> results = new Runner(options).run();
        BenchmarkTables tables = BenchmarkTables.from(results);
        try {
            Files.createDirectories(REPORT_DIR);
            tables.writeSummary(REPORT_DIR.resolve(SUMMARY_FILE));
            tables.writeCrossoverSummary(REPORT_DIR.resolve(CROSSOVER_FILE));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist benchmark summaries", e);
        }
    }

    private static Options buildOptions() {
        int warmups = Integer.getInteger("lab2.jmh.warmups", 1);
        int measurements = Integer.getInteger("lab2.jmh.measurements", 1);
        int forks = Integer.getInteger("lab2.jmh.forks", 1);

        return new OptionsBuilder()
                .include(ReceiptStatisticsBenchmark.class.getSimpleName())
                .include(StatisticsMicroBenchmarks.class.getSimpleName())
                .warmupIterations(warmups)
                .measurementIterations(measurements)
                .forks(forks)
                .shouldFailOnError(true)
                .build();
    }

    private record Measurement(double meanMillis, double errorMillis, String unit) {

        Measurement withUnit(String desiredUnit) {
            if (Objects.equals(unit, desiredUnit)) {
                return this;
            }
            // fall back to string comparison; JMH already configured to ms/op
            if ("ms/op".equals(unit) && "ms/op".equals(desiredUnit)) {
                return this;
            }
            throw new IllegalArgumentException("Unexpected JMH unit conversion: " + unit + " -> " + desiredUnit);
        }
    }

    private static final class BenchmarkTables {
        private final Map<Long, TreeMap<Integer, TreeMap<String, Measurement>>> byDelayMillis;

        private BenchmarkTables(Map<Long, TreeMap<Integer, TreeMap<String, Measurement>>> byDelayMillis) {
            this.byDelayMillis = byDelayMillis;
        }

        static BenchmarkTables from(Collection<RunResult> results) {
            Map<Long, TreeMap<Integer, TreeMap<String, Measurement>>> data = new TreeMap<>();
            for (RunResult result : results) {
                int datasetSize = Integer.parseInt(result.getParams().getParam("datasetSize"));
                String delayParam = result.getParams().getParam("itemNameDelayMillis");
                long delayMillis = delayParam != null ? Long.parseLong(delayParam) : 0L;
                String aggregatorLabel = result.getParams().getBenchmark();
                if (aggregatorLabel == null || aggregatorLabel.isBlank()) {
                    aggregatorLabel = result.getPrimaryResult().getLabel();
                }
                if (aggregatorLabel != null) {
                    int lastDot = aggregatorLabel.lastIndexOf('.');
                    if (lastDot > 0 && lastDot + 1 < aggregatorLabel.length()) {
                        String method = aggregatorLabel.substring(lastDot + 1);
                        int beforeDot = aggregatorLabel.lastIndexOf('.', lastDot - 1);
                        String clazz = beforeDot >= 0
                                ? aggregatorLabel.substring(beforeDot + 1, lastDot)
                                : aggregatorLabel.substring(0, lastDot);
                        aggregatorLabel = clazz + "." + method;
                    }
                }

                if (aggregatorLabel == null || aggregatorLabel.isBlank()) {
                    continue;
                }

                Measurement measurement = extractMeasurement(result);
                TreeMap<Integer, TreeMap<String, Measurement>> bySize = data
                        .computeIfAbsent(delayMillis, key -> new TreeMap<>());
                TreeMap<String, Measurement> byAggregator = bySize
                        .computeIfAbsent(datasetSize, key -> new TreeMap<>());
                byAggregator.put(aggregatorLabel, measurement);
            }
            return new BenchmarkTables(data);
        }

        void writeSummary(Path target) throws IOException {
            try (var writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                writer.write("delayMillis,datasetSize,aggregator,meanMillis,errorMillis,unit");
                writer.newLine();

                for (Map.Entry<Long, TreeMap<Integer, TreeMap<String, Measurement>>> delayEntry : byDelayMillis
                        .entrySet()) {
                    long delay = delayEntry.getKey();
                    for (Map.Entry<Integer, TreeMap<String, Measurement>> sizeEntry : delayEntry.getValue()
                            .entrySet()) {
                        int dataset = sizeEntry.getKey();
                        for (Map.Entry<String, Measurement> aggregatorEntry : sizeEntry.getValue().entrySet()) {
                            emitSummaryRow(writer, delay, dataset, aggregatorEntry.getKey(),
                                    aggregatorEntry.getValue());
                        }
                    }
                }
            }
        }

        void writeCrossoverSummary(Path target) throws IOException {
            try (var writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                writer.write(
                        "delayMillis,type,estimatedDatasetSize,lowerDatasetSize,lowerSequentialMillis,lowerParallelMillis,"
                                + "upperDatasetSize,upperSequentialMillis,upperParallelMillis,note");
                writer.newLine();

                for (Map.Entry<Long, TreeMap<Integer, TreeMap<String, Measurement>>> delayEntry : byDelayMillis
                        .entrySet()) {
                    long delay = delayEntry.getKey();
                    TreeMap<Integer, AggregatedRow> rows = extractReceiptStatisticsRows(delayEntry.getValue());
                    CrossoverResult crossover = CrossoverResult.from(rows);
                    writer.write(crossover.toCsvLine(delay));
                    writer.newLine();
                }
            }
        }

        private static TreeMap<Integer, AggregatedRow> extractReceiptStatisticsRows(
                TreeMap<Integer, TreeMap<String, Measurement>> table) {
            TreeMap<Integer, AggregatedRow> rows = new TreeMap<>();
            for (Map.Entry<Integer, TreeMap<String, Measurement>> entry : table.entrySet()) {
                Measurement sequential = firstNonNull(entry.getValue(),
                        "StatisticsMicroBenchmarks.receiptStatisticsSequential",
                        "ReceiptStatisticsBenchmark.sequentialStream");
                Measurement parallel = firstNonNull(entry.getValue(),
                        "StatisticsMicroBenchmarks.receiptStatisticsParallel",
                        "ReceiptStatisticsBenchmark.parallelStream");
                if (sequential == null || parallel == null) {
                    continue;
                }
                Measurement spliterator = firstNonNull(entry.getValue(),
                        "StatisticsMicroBenchmarks.receiptStatisticsSpliterator",
                        "ReceiptStatisticsBenchmark.parallelStreamWithCustomSpliterator");
                rows.put(entry.getKey(), new AggregatedRow(sequential, parallel, spliterator));
            }
            return rows;
        }

        private static void emitSummaryRow(
                java.io.BufferedWriter writer,
                long delay,
                int dataset,
                String aggregator,
                Measurement measurement) throws IOException {

            if (measurement == null) {
                return;
            }
            writer.write(String.format(Locale.ROOT,
                    "%d,%d,%s,%.6f,%.6f,%s",
                    delay,
                    dataset,
                    aggregator,
                    measurement.meanMillis,
                    measurement.errorMillis,
                    measurement.unit));
            writer.newLine();
        }

        private static Measurement extractMeasurement(RunResult result) {
            var primary = result.getPrimaryResult();
            var statistics = primary.getStatistics();
            double mean = statistics.getMean();
            double error = statistics.getMeanErrorAt(0.05);
            String unit = primary.getScoreUnit();
            return new Measurement(mean, error, unit).withUnit("ms/op");
        }

        private static Measurement firstNonNull(Map<String, Measurement> measurements, String... keys) {
            for (String key : keys) {
                Measurement measurement = measurements.get(key);
                if (measurement != null) {
                    return measurement;
                }
            }
            return null;
        }
    }

    private record CrossoverResult(
            Type type,
            OptionalDouble estimatedDatasetSize,
            Optional<Boundary> lower,
            Optional<Boundary> upper,
            String note) {

        enum Type {
            EXACT, INTERPOLATED, NONE
        }

        static CrossoverResult from(TreeMap<Integer, AggregatedRow> rows) {
            if (rows.isEmpty()) {
                return new CrossoverResult(Type.NONE, OptionalDouble.empty(), Optional.empty(), Optional.empty(),
                        "no data");
            }

            Boundary previous = null;
            for (Map.Entry<Integer, AggregatedRow> entry : rows.entrySet()) {
                Boundary current = Boundary.from(entry.getKey(), entry.getValue());
                if (!current.isComparable()) {
                    continue;
                }
                if (previous != null) {
                    double prevDiff = previous.diff();
                    double currentDiff = current.diff();

                    if (Math.abs(currentDiff) < 1e-6) {
                        return exact(current);
                    }
                    if (Math.abs(prevDiff) < 1e-6) {
                        return exact(previous);
                    }
                    if (Math.signum(prevDiff) != Math.signum(currentDiff)) {
                        double estimated = interpolate(previous, current);
                        return interpolated(previous, current, estimated);
                    }
                }
                previous = current;
            }

            Boundary first = rows.entrySet().stream()
                    .map(entry -> Boundary.from(entry.getKey(), entry.getValue()))
                    .filter(Boundary::isComparable)
                    .findFirst()
                    .orElse(null);
            Boundary last = rows.descendingMap().entrySet().stream()
                    .map(entry -> Boundary.from(entry.getKey(), entry.getValue()))
                    .filter(Boundary::isComparable)
                    .findFirst()
                    .orElse(null);

            if (first == null || last == null) {
                return new CrossoverResult(
                        Type.NONE,
                        OptionalDouble.empty(),
                        Optional.ofNullable(first),
                        Optional.ofNullable(last),
                        "insufficient data");
            }
            String trend = first.diff() < 0 && last.diff() < 0
                    ? "parallel faster across tested range"
                    : first.diff() > 0 && last.diff() > 0
                            ? "sequential faster across tested range"
                            : "no crossover detected";
            return new CrossoverResult(Type.NONE, OptionalDouble.empty(), Optional.of(first), Optional.of(last), trend);
        }

        private static CrossoverResult exact(Boundary boundary) {
            return new CrossoverResult(
                    Type.EXACT,
                    OptionalDouble.of(boundary.datasetSize()),
                    Optional.of(boundary),
                    Optional.of(boundary),
                    "exact match at measured dataset size");
        }

        private static CrossoverResult interpolated(Boundary lower, Boundary upper, double estimatedDataset) {
            return new CrossoverResult(
                    Type.INTERPOLATED,
                    OptionalDouble.of(estimatedDataset),
                    Optional.of(lower),
                    Optional.of(upper),
                    "linear interpolation between neighbouring dataset sizes");
        }

        private static double interpolate(Boundary lower, Boundary upper) {
            double lowerDiff = lower.diff();
            double upperDiff = upper.diff();
            double range = upper.datasetSize() - lower.datasetSize();
            if (range <= 0) {
                return lower.datasetSize();
            }
            double fraction = Math.abs(lowerDiff) / (Math.abs(lowerDiff) + Math.abs(upperDiff));
            return lower.datasetSize() + fraction * range;
        }

        String toCsvLine(long delayMillis) {
            DecimalFormat df = new DecimalFormat("0.######", DecimalFormatSymbolsHolder.SYMBOLS);
            String estimated = estimatedDatasetSize.isPresent()
                    ? df.format(estimatedDatasetSize.getAsDouble())
                    : "";
            String lowerValues = lower.map(boundary -> boundary.toCsv(df)).orElse(",,");
            String upperValues = upper.map(boundary -> boundary.toCsv(df)).orElse(",,");
            return String.join(",",
                    Long.toString(delayMillis),
                    type.name().toLowerCase(Locale.ROOT),
                    estimated,
                    lowerValues,
                    upperValues,
                    quote(note));
        }

        private static String quote(String text) {
            if (text == null || text.isBlank()) {
                return "";
            }
            String sanitized = text.replace("\"", "\"\"");
            return "\"" + sanitized + "\"";
        }
    }

    private record Boundary(int datasetSize, double sequentialMillis, double parallelMillis) {

        static Boundary from(int datasetSize, AggregatedRow row) {
            Measurement sequential = row.sequential;
            Measurement parallel = row.parallel;
            return new Boundary(
                    datasetSize,
                    sequential != null ? sequential.meanMillis : Double.NaN,
                    parallel != null ? parallel.meanMillis : Double.NaN);
        }

        boolean isComparable() {
            return !Double.isNaN(sequentialMillis) && !Double.isNaN(parallelMillis);
        }

        double diff() {
            return sequentialMillis - parallelMillis;
        }

        String toCsv(DecimalFormat df) {
            return datasetSize + ","
                    + df.format(sequentialMillis) + ","
                    + df.format(parallelMillis);
        }
    }

    private static final class DecimalFormatSymbolsHolder {
        private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.ROOT);

        private DecimalFormatSymbolsHolder() {
        }
    }

    private record AggregatedRow(Measurement sequential, Measurement parallel, Measurement spliterator) {
    }
}
