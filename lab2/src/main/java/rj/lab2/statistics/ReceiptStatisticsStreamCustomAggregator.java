package rj.lab1.statistics;

import java.util.List;

import rj.lab1.model.Receipt;
import rj.lab1.statistics.Collectors.ReceiptStatisticsCollector;

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
