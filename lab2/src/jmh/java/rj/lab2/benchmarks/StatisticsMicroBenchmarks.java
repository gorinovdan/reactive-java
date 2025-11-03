package rj.lab2.benchmarks;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import rj.lab2.generators.SimpleReceiptGenerator;
import rj.lab2.model.Receipt;
import rj.lab2.statistics.ItemAverageReceipt;
import rj.lab2.statistics.ItemSales;
import rj.lab2.statistics.ReceiptStatistics;
import rj.lab2.statistics.aggregators.complex.ReceiptStatisticsSpliteratorAggregator;
import rj.lab2.statistics.aggregators.complex.ReceiptStatisticsStreamAggregator;
import rj.lab2.statistics.aggregators.item_average_receipt.ItemAverageStreamCustomAggregator;
import rj.lab2.statistics.aggregators.top_items_by_quantity.TopItemsByQuantityAggregator;
import rj.lab2.statistics.aggregators.total_revenue.TotalRevenueAggregators;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class StatisticsMicroBenchmarks {

    private static final int SPLITERATOR_MIN_BATCH = 256;

    @Param({ "5000", "25000", "250000" })
    private int datasetSize;

    @Param({ "1" })
    private long itemNameDelayMillis;

    private List<Receipt> receipts;

    @Setup(Level.Trial)
    public void setup() {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);
        receipts = generator.generateMany(datasetSize);
    }

    // --- Total revenue ---

    @Benchmark
    public double totalRevenueCircle() {
        return TotalRevenueAggregators.circleAggregate(receipts);
    }

    @Benchmark
    public double totalRevenueStream() {
        return TotalRevenueAggregators.streamAggregate(receipts);
    }

    @Benchmark
    public double totalRevenueCollector() {
        return TotalRevenueAggregators.customCollectorAggregate(receipts);
    }

    // --- Top items by quantity ---

    @Benchmark
    public List<ItemSales> topItemsCircle() {
        return TopItemsByQuantityAggregator.topItemsByQuantityCircleAggregator(receipts, itemNameDelayMillis);
    }

    @Benchmark
    public List<ItemSales> topItemsStream() {
        return TopItemsByQuantityAggregator.topItemsByQuantityStreamAggregator(receipts, itemNameDelayMillis);
    }

    @Benchmark
    public List<ItemSales> topItemsCollector() {
        return TopItemsByQuantityAggregator.topItemsByQuantity(receipts, itemNameDelayMillis);
    }

    // --- Item average receipt ---

    @Benchmark
    public List<ItemAverageReceipt> itemAverageSequential() {
        return ItemAverageStreamCustomAggregator.aggregateSequential(receipts, itemNameDelayMillis);
    }

    @Benchmark
    public List<ItemAverageReceipt> itemAverageParallel() {
        return ItemAverageStreamCustomAggregator.aggregateParallel(receipts, itemNameDelayMillis);
    }

    // --- Receipt statistics ---

    @Benchmark
    public ReceiptStatistics receiptStatisticsSequential() {
        return ReceiptStatisticsStreamAggregator.aggregateSequential(receipts, itemNameDelayMillis);
    }

    @Benchmark
    public ReceiptStatistics receiptStatisticsParallel() {
        return ReceiptStatisticsStreamAggregator.aggregateParallel(receipts, itemNameDelayMillis);
    }

    @Benchmark
    public ReceiptStatistics receiptStatisticsSpliterator() {
        return ReceiptStatisticsSpliteratorAggregator.aggregateParallel(
                receipts,
                itemNameDelayMillis,
                SPLITERATOR_MIN_BATCH);
    }
}
