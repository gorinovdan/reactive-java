package rj.lab2.statistics.aggregators.item_average_receipt;

import java.util.List;

import rj.lab2.model.Receipt;
import rj.lab2.statistics.ItemAverageReceipt;

public final class ItemAverageStreamCustomAggregator {

    private ItemAverageStreamCustomAggregator() {
    }

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts) {
        return aggregateParallel(receipts, 0L);
    }

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts, long itemNameDelayMillis) {
        return aggregateParallel(receipts, itemNameDelayMillis);
    }

    public static List<ItemAverageReceipt> aggregateSequential(List<Receipt> receipts, long itemNameDelayMillis) {
        return receipts.stream().collect(new ItemAverageCollector(itemNameDelayMillis));
    }

    public static List<ItemAverageReceipt> aggregateParallel(List<Receipt> receipts, long itemNameDelayMillis) {
        return receipts.parallelStream().collect(new ItemAverageCollector(itemNameDelayMillis));
    }
}
