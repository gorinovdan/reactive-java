package rj.lab2.statistics;

import java.util.Comparator;

/**
 * Aggregated information about revenue generated in a city.
 */
public record CityRevenue(String city, double totalRevenue, long ordersCount) {

    public static Comparator<CityRevenue> byRevenueAndOrdersDescending() {
        return Comparator.comparingDouble(CityRevenue::totalRevenue)
                .reversed()
                .thenComparing(Comparator.comparingLong(CityRevenue::ordersCount).reversed())
                .thenComparing(CityRevenue::city);
    }
}
