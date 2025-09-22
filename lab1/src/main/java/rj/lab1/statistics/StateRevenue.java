package rj.lab1.statistics;

import java.util.Comparator;

/**
 * Revenue metrics grouped by state/region.
 */
public record StateRevenue(String state, double totalRevenue, long ordersCount) {

    public static Comparator<StateRevenue> byRevenueDescending() {
        return Comparator.comparingDouble(StateRevenue::totalRevenue)
                .reversed()
                .thenComparing(Comparator.comparingLong(StateRevenue::ordersCount).reversed())
                .thenComparing(StateRevenue::state);
    }
}
