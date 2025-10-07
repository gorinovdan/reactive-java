package rj.lab1.statistics;

import java.util.List;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import rj.lab1.model.Receipt;
import rj.lab1.statistics.spliterator.ReceiptSpliterator;

public final class ReceiptStatisticsSpliteratorAggregator {

    private static final int DEFAULT_MIN_BATCH = 256;

    private ReceiptStatisticsSpliteratorAggregator() {
    }

    public static ReceiptStatistics aggregateParallel(List<Receipt> receipts, long itemNameDelayMillis) {
        return aggregateParallel(receipts, itemNameDelayMillis, DEFAULT_MIN_BATCH);
    }

    public static ReceiptStatistics aggregateParallel(
            List<Receipt> receipts,
            long itemNameDelayMillis,
            int minimumBatchSize) {

        Spliterator<Receipt> spliterator = new ReceiptSpliterator(receipts, minimumBatchSize);
        return ReceiptStatisticsStreamAggregator.collect(
                StreamSupport.stream(spliterator, true),
                itemNameDelayMillis);
    }
}
