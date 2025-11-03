package rj.lab2.statistics.aggregators.complex;

import java.util.List;
import java.util.stream.Stream;

import rj.lab2.model.Receipt;
import rj.lab2.statistics.ReceiptStatistics;

public final class ReceiptStatisticsStreamAggregator {

    private ReceiptStatisticsStreamAggregator() {
    }

    public static ReceiptStatistics aggregate(List<Receipt> receipts) {
        return aggregateParallel(receipts, 0L);
    }

    public static ReceiptStatistics aggregate(List<Receipt> receipts, long itemNameDelayMillis) {
        return aggregateParallel(receipts, itemNameDelayMillis);
    }

    public static ReceiptStatistics aggregateSequential(List<Receipt> receipts, long itemNameDelayMillis) {
        return collect(receipts.stream(), itemNameDelayMillis);
    }

    public static ReceiptStatistics aggregateParallel(List<Receipt> receipts, long itemNameDelayMillis) {
        return collect(receipts.parallelStream(), itemNameDelayMillis);
    }

    static ReceiptStatistics collect(Stream<Receipt> stream, long itemNameDelayMillis) {
        return stream.collect(ReceiptStatisticsCollector.withItemNameDelay(itemNameDelayMillis));
    }

    static ReceiptStatistics collect(Stream<Receipt> stream) {
        return collect(stream, 0L);
    }
}
