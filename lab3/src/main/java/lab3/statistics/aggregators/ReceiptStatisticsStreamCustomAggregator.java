package lab3.statistics.aggregators;

import java.util.List;

import lab3.model.Receipt;
import lab3.statistics.Collectors.ReceiptStatisticsCollector;
import lab3.statistics.model.ReceiptStatistics;

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
