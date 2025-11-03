package rj.lab2.statistics;

import java.util.Comparator;

/**
 * Aggregated information about sales of a particular item.
 */
public record ItemSales(String itemName, long quantitySold, double totalRevenue) {

    public static Comparator<ItemSales> byQuantityAndRevenueDescending() {
        return Comparator.comparingLong(ItemSales::quantitySold)
                .reversed()
                .thenComparing(Comparator.comparingDouble(ItemSales::totalRevenue).reversed())
                .thenComparing(ItemSales::itemName);
    }
}
