package rj.lab2.statistics;

import java.util.Comparator;

/**
 * Aggregated sales metrics grouped by a price tier.
 */
public record PriceTierSales(PriceTier priceTier, long itemsSold, double totalRevenue, double averageUnitPrice) {

    public static Comparator<PriceTierSales> byRevenueDescending() {
        return Comparator.comparingDouble(PriceTierSales::totalRevenue)
                .reversed()
                .thenComparing(Comparator.comparingLong(PriceTierSales::itemsSold).reversed())
                .thenComparing(priceTierSales -> priceTierSales.priceTier().name());
    }
}
