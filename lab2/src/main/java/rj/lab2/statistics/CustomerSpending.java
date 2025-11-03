package rj.lab2.statistics;

import java.util.Comparator;

/**
 * Aggregated information about how much a customer has spent.
 */
public record CustomerSpending(String customerName, double totalSpent) {

    public static Comparator<CustomerSpending> byTotalSpentDescending() {
        return Comparator.comparingDouble(CustomerSpending::totalSpent)
                .reversed()
                .thenComparing(CustomerSpending::customerName);
    }
}
