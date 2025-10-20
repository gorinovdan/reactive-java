package lab3.statistics.model;

import java.util.Comparator;

import lab3.model.ReceiptStatus;

/**
 * Combined revenue and volume metrics for a particular order status.
 */
public record StatusRevenue(ReceiptStatus status, double totalRevenue, long ordersCount, double averageOrderValue) {

    public static Comparator<StatusRevenue> byRevenueDescending() {
        return Comparator.comparingDouble(StatusRevenue::totalRevenue)
                .reversed()
                .thenComparing(Comparator.comparingLong(StatusRevenue::ordersCount).reversed())
                .thenComparing(statusRevenue -> statusRevenue.status().name());
    }
}
