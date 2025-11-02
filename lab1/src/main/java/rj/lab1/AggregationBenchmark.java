package rj.lab1;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.aggregators.complex.ReceiptStatisticsIterateCircleAggregator;
import rj.lab1.statistics.aggregators.complex.ReceiptStatisticsStreamAggregator;
import rj.lab1.statistics.aggregators.complex.ReceiptStatisticsStreamCustomAggregator;
import rj.lab1.statistics.aggregators.item_average_receipt.ItemAverageReceiptCircleAggregator;
import rj.lab1.statistics.aggregators.item_average_receipt.ItemAverageReceiptStreamAggregator;
import rj.lab1.statistics.aggregators.item_average_receipt.ItemAverageStreamCustomAggregator;
import rj.lab1.statistics.aggregators.top_items_by_quantity.TopItemsByQuantityAggregator;
import rj.lab1.statistics.aggregators.total_revenue.TotalRevenueAggregators;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

public class AggregationBenchmark {

    private static void benchmark(String label, Function<List<Receipt>, ?> aggregator, List<Receipt> receipts) {
        Instant start = Instant.now();
        aggregator.apply(receipts);
        Instant end = Instant.now();
        System.out.printf("%-40s -> %6d ms%n", label, Duration.between(start, end).toMillis());
    }

    public static void main(String[] args) {
        int[] sizes = {5_000, 25_000, 250_000};

        SimpleReceiptGenerator simpleGen = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);

        for (int size : sizes) {
            System.out.println("\n===== Benchmark for " + size + " receipts =====");
            List<Receipt> receipts = simpleGen.generateMany(size);

            // --- Total Revenue ---
            benchmark("TotalRevenue.circleAggregate", TotalRevenueAggregators::circleAggregate, receipts);
            benchmark("TotalRevenue.streamAggregate", TotalRevenueAggregators::streamAggregate, receipts);
            benchmark("TotalRevenue.customCollectorAggregate", TotalRevenueAggregators::customCollectorAggregate, receipts);

            // --- Top Items by Quantity ---
            benchmark("TopItemsByQuantity.circle", TopItemsByQuantityAggregator::topItemsByQuantityCircleAggregator, receipts);
            benchmark("TopItemsByQuantity.stream", TopItemsByQuantityAggregator::topItemsByQuantityStreamAggregator, receipts);
            benchmark("TopItemsByQuantity.collector", TopItemsByQuantityAggregator::topItemsByQuantity, receipts);

            // --- Item Average Receipt ---
            benchmark("ItemAverageReceipt.circle", ItemAverageReceiptCircleAggregator::aggregate, receipts);
            benchmark("ItemAverageReceipt.stream", ItemAverageReceiptStreamAggregator::aggregate, receipts);
            benchmark("ItemAverageReceipt.collector", ItemAverageStreamCustomAggregator::aggregate, receipts);

            // --- Complex ---
            benchmark("ReceiptStatistics.circle", ReceiptStatisticsIterateCircleAggregator::aggregate, receipts);
            benchmark("ReceiptStatistics.stream", ReceiptStatisticsStreamAggregator::aggregate, receipts);
            benchmark("ReceiptStatistics.collector", ReceiptStatisticsStreamCustomAggregator::aggregate, receipts);
        }
    }
}

