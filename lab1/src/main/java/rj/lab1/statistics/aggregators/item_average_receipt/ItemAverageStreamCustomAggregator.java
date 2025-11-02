package rj.lab1.statistics.aggregators.item_average_receipt;

import rj.lab1.model.Receipt;
import rj.lab1.statistics.ItemAverageReceipt;

import java.util.List;

public class ItemAverageStreamCustomAggregator {
    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts) {
        return receipts.stream().collect(new ItemAverageCollector());
    }

}
