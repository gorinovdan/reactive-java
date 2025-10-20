package lab3.statistics.model;

/**
 * Summary information about the overall average receipt amount across all orders.
 */
public record TotalAverage(long receiptCount, double totalRevenue, double averageReceiptAmount) {

    public static TotalAverage empty() {
        return new TotalAverage(0L, 0.0, 0.0);
    }
}

