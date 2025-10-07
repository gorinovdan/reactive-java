package rj.lab1.statistics;

import java.util.Comparator;

/**
 * Aggregated information about the average receipt amount for orders
 * containing a particular item.
 */
public record ItemAverageReceipt(String itemName, long receiptCount, double averageReceiptAmount) {

    public static Comparator<ItemAverageReceipt> byAverageReceiptDescending() {
        return Comparator.comparingDouble(ItemAverageReceipt::averageReceiptAmount)
                .reversed()
                .thenComparing(Comparator.comparingLong(ItemAverageReceipt::receiptCount).reversed())
                .thenComparing(ItemAverageReceipt::itemName);
    }
}
