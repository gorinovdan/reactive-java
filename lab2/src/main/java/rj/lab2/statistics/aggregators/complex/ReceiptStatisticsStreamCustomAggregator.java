package rj.lab2.statistics.aggregators.complex;

import java.util.List;

import rj.lab2.model.Receipt;
import rj.lab2.statistics.ReceiptStatistics;

public class ReceiptStatisticsStreamCustomAggregator {

    public static ReceiptStatistics aggregate(List<Receipt> receipts) {
        return aggregateParallel(receipts, 0L);
    }

    public static ReceiptStatistics aggregate(List<Receipt> receipts, long itemNameDelayMillis) {
        return aggregateParallel(receipts, itemNameDelayMillis);
    }

    public static ReceiptStatistics aggregateSequential(List<Receipt> receipts, long itemNameDelayMillis) {
        return receipts.stream().collect(ReceiptStatisticsCollector.withItemNameDelay(itemNameDelayMillis));
    }

    public static ReceiptStatistics aggregateParallel(List<Receipt> receipts, long itemNameDelayMillis) {
        return receipts.parallelStream().collect(ReceiptStatisticsCollector.withItemNameDelay(itemNameDelayMillis));
    }
}
