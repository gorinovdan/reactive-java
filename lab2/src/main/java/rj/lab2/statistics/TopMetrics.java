package rj.lab1.statistics;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;

/**
 * Utility class that turns aggregated maps into sorted top metrics lists.
 */
public final class TopMetrics {

    public static final int DEFAULT_TOP_LIMIT = 5;

    private TopMetrics() {
    }

    public static List<CustomerSpending> calculateTopCustomers(Map<String, Double> spendingByCustomer) {
        return calculateTopCustomers(spendingByCustomer, DEFAULT_TOP_LIMIT);
    }

    public static List<CustomerSpending> calculateTopCustomers(Map<String, Double> spendingByCustomer, int limit) {
        return spendingByCustomer.entrySet().stream()
                .map(entry -> new CustomerSpending(entry.getKey(), entry.getValue()))
                .sorted(CustomerSpending.byTotalSpentDescending())
                .limit(limit)
                .toList();
    }

    public static List<ItemSales> calculateTopItems(Map<String, Long> quantityByItem,
            Map<String, Double> revenueByItem) {
        return calculateTopItems(quantityByItem, revenueByItem, DEFAULT_TOP_LIMIT);
    }

    public static List<ItemSales> calculateTopItems(
            Map<String, Long> quantityByItem,
            Map<String, Double> revenueByItem,
            int limit) {
        return quantityByItem.entrySet().stream()
                .map(entry -> new ItemSales(
                        entry.getKey(),
                        entry.getValue(),
                        revenueByItem.getOrDefault(entry.getKey(), 0.0)))
                .sorted(ItemSales.byQuantityAndRevenueDescending())
                .limit(limit)
                .toList();
    }

    public static List<ItemAverageReceipt> calculateItemAverageReceipts(List<Receipt> receipts) {
        return ItemAverageReceiptMetrics.calculate(receipts);
    }

    public static List<ItemAverageReceipt> calculateItemAverageReceipts(
            List<Receipt> receipts,
            long itemNameDelayMillis) {
        return ItemAverageReceiptMetrics.calculate(receipts, itemNameDelayMillis);
    }

    public static List<CityRevenue> calculateTopCities(Map<String, Double> revenueByCity,
            Map<String, Long> ordersByCity) {
        return calculateTopCities(revenueByCity, ordersByCity, DEFAULT_TOP_LIMIT);
    }

    public static List<CityRevenue> calculateTopCities(
            Map<String, Double> revenueByCity,
            Map<String, Long> ordersByCity,
            int limit) {
        return revenueByCity.entrySet().stream()
                .map(entry -> new CityRevenue(
                        entry.getKey(),
                        entry.getValue(),
                        ordersByCity.getOrDefault(entry.getKey(), 0L)))
                .sorted(CityRevenue.byRevenueAndOrdersDescending())
                .limit(limit)
                .toList();
    }

    public static List<CustomerOrderProfile> calculateTopCustomersByOrders(
            Map<String, Long> ordersByCustomer,
            Map<String, Double> revenueByCustomer) {
        return calculateTopCustomersByOrders(ordersByCustomer, revenueByCustomer, DEFAULT_TOP_LIMIT);
    }

    public static List<CustomerOrderProfile> calculateTopCustomersByOrders(
            Map<String, Long> ordersByCustomer,
            Map<String, Double> revenueByCustomer,
            int limit) {
        return ordersByCustomer.entrySet().stream()
                .map(entry -> {
                    String customer = entry.getKey();
                    long orders = entry.getValue();
                    double totalSpent = revenueByCustomer.getOrDefault(customer, 0.0);
                    double averageOrderValue = orders > 0 ? totalSpent / orders : 0.0;
                    return new CustomerOrderProfile(customer, orders, totalSpent, averageOrderValue);
                })
                .sorted(CustomerOrderProfile.byOrdersAndSpendingDescending())
                .limit(limit)
                .toList();
    }

    public static List<StatusRevenue> calculateStatusRevenue(
            Map<ReceiptStatus, Double> revenueByStatus,
            Map<ReceiptStatus, Long> ordersByStatus) {
        EnumSet<ReceiptStatus> statuses = EnumSet.noneOf(ReceiptStatus.class);
        statuses.addAll(revenueByStatus.keySet());
        statuses.addAll(ordersByStatus.keySet());

        return statuses.stream()
                .map(status -> {
                    double revenue = revenueByStatus.getOrDefault(status, 0.0);
                    long orders = ordersByStatus.getOrDefault(status, 0L);
                    double averageOrderValue = orders > 0 ? revenue / orders : 0.0;
                    return new StatusRevenue(status, revenue, orders, averageOrderValue);
                })
                .sorted(StatusRevenue.byRevenueDescending())
                .toList();
    }

    public static List<PriceTierSales> calculatePriceTierSales(
            Map<PriceTier, Long> quantityByTier,
            Map<PriceTier, Double> revenueByTier) {
        Set<PriceTier> tiers = EnumSet.allOf(PriceTier.class);
        return tiers.stream()
                .map(tier -> {
                    long quantity = quantityByTier.getOrDefault(tier, 0L);
                    double revenue = revenueByTier.getOrDefault(tier, 0.0);
                    double averageUnitPrice = quantity > 0 ? revenue / quantity : 0.0;
                    return new PriceTierSales(tier, quantity, revenue, averageUnitPrice);
                })
                .filter(sales -> sales.itemsSold() > 0 || sales.totalRevenue() > 0)
                .sorted(PriceTierSales.byRevenueDescending())
                .toList();
    }

    public static List<StateRevenue> calculateTopStates(
            Map<String, Double> revenueByState,
            Map<String, Long> ordersByState) {
        return revenueByState.entrySet().stream()
                .map(entry -> new StateRevenue(
                        entry.getKey(),
                        entry.getValue(),
                        ordersByState.getOrDefault(entry.getKey(), 0L)))
                .sorted(StateRevenue.byRevenueDescending())
                .limit(DEFAULT_TOP_LIMIT)
                .toList();
    }
}
