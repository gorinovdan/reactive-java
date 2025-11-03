package rj.lab2.statistics;

/**
 * Summary information about the overall average receipt amount across all orders.
 *
 * @param receiptCount         number of receipts that contributed to the average
 * @param totalRevenue         aggregated revenue across those receipts
 * @param averageReceiptAmount overall average receipt amount
 */
public record TotalAverage(long receiptCount, double totalRevenue, double averageReceiptAmount) {

    public static TotalAverage empty() {
        return new TotalAverage(0L, 0.0, 0.0);
    }
}
