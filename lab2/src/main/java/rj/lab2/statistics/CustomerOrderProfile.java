package rj.lab2.statistics;

import java.util.Comparator;

/**
 * Customer-centric metric that tracks how frequently the customer orders and
 * how much they spend.
 */
public record CustomerOrderProfile(
        String customerName,
        long ordersCount,
        double totalSpent,
        double averageReceiptAmount) {

    public static Comparator<CustomerOrderProfile> byOrdersAndSpendingDescending() {
        return Comparator.comparingLong(CustomerOrderProfile::ordersCount)
                .reversed()
                .thenComparing(Comparator.comparingDouble(CustomerOrderProfile::totalSpent).reversed())
                .thenComparing(CustomerOrderProfile::customerName);
    }
}
