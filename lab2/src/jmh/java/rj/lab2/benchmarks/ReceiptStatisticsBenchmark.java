package rj.lab2.benchmarks;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatistics;
import rj.lab1.statistics.ReceiptStatisticsSpliteratorAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamAggregator;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReceiptStatisticsBenchmark {

    private static final int SPLITERATOR_MIN_BATCH = 256;

    @Param({ "1000", "5000", "20000", "50000", "100000", "250000" })
    private int datasetSize;

    @Param({ "0", "5" })
    private long itemNameDelayMillis;

    private List<Receipt> receipts;

    @Setup(Level.Trial)
    public void setup() {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);
        receipts = generator.generateMany(datasetSize);
    }

    @Benchmark
    public ReceiptStatistics sequentialStream() {
        return ReceiptStatisticsStreamAggregator.aggregateSequential(receipts, itemNameDelayMillis);
    }

    @Benchmark
    public ReceiptStatistics parallelStream() {
        return ReceiptStatisticsStreamAggregator.aggregateParallel(receipts, itemNameDelayMillis);
    }

    @Benchmark
    public ReceiptStatistics parallelStreamWithCustomSpliterator() {
        return ReceiptStatisticsSpliteratorAggregator.aggregateParallel(
                receipts,
                itemNameDelayMillis,
                SPLITERATOR_MIN_BATCH);
    }
}
