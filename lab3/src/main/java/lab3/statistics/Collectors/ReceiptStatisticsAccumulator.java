package lab3.statistics.Collectors;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lab3.model.Item;
import lab3.model.Receipt;
import lab3.model.ReceiptStatus;
import lab3.statistics.model.ItemAverageReceiptMetrics;
import lab3.statistics.model.PriceTier;
import lab3.statistics.model.ReceiptStatistics;
import lab3.statistics.model.TotalAverage;
import lab3.statistics.model.TotalAverageMetrics;
import lab3.statistics.model.TopMetrics;

public class ReceiptStatisticsAccumulator {
    private final long itemNameDelayMillis;

    long totalOrders = 0;
    double totalRevenue = 0;
    double minReceipt = Double.POSITIVE_INFINITY;
    double maxReceipt = Double.NEGATIVE_INFINITY;

    long totalItemsSold = 0;
    long totalLoyaltyPoints = 0;

    Map<ReceiptStatus, Long> ordersByStatus = new ConcurrentHashMap<>();
    Map<Integer, Double> revenueByMonth = new ConcurrentHashMap<>();
    Set<String> uniqueCustomers = ConcurrentHashMap.newKeySet();
    Map<String, Double> revenueByCustomer = new ConcurrentHashMap<>();
    Map<String, Long> ordersByCustomer = new ConcurrentHashMap<>();
    Map<String, Long> itemQuantityByName = new ConcurrentHashMap<>();
    Map<String, Double> itemRevenueByName = new ConcurrentHashMap<>();
    Map<String, Double> revenueByCity = new ConcurrentHashMap<>();
    Map<String, Long> ordersByCity = new ConcurrentHashMap<>();
    Map<ReceiptStatus, Double> revenueByStatus = new ConcurrentHashMap<>();
    Map<PriceTier, Long> quantityByPriceTier = new ConcurrentHashMap<>();
    Map<PriceTier, Double> revenueByPriceTier = new ConcurrentHashMap<>();
    Map<String, Double> revenueByState = new ConcurrentHashMap<>();
    Map<String, Long> ordersByState = new ConcurrentHashMap<>();
    ItemAverageReceiptMetrics.ItemAverageAccumulator itemAverageAccumulator;
    TotalAverageMetrics.TotalAverageAccumulator totalAverageAccumulator;

    public ReceiptStatisticsAccumulator() {
        this(0L);
    }

    public ReceiptStatisticsAccumulator(long itemNameDelayMillis) {
        this.itemNameDelayMillis = Math.max(0L, itemNameDelayMillis);
        this.itemAverageAccumulator = ItemAverageReceiptMetrics.newAccumulator(this::resolveItemName);
        this.totalAverageAccumulator = TotalAverageMetrics.newAccumulator();
    }

    void add(Receipt r) {
        totalOrders++;

        double orderTotal = 0;
        long itemsInOrder = 0;
        Set<String> itemsInReceipt = new HashSet<>();
        for (Item item : r.getItems()) {
            String itemName = resolveItemName(item);
            double itemRevenue = item.getUnitPrice() * item.getQuantity();
            orderTotal += itemRevenue;
            itemsInOrder += item.getQuantity();
            itemQuantityByName.merge(itemName, (long) item.getQuantity(), Long::sum);
            itemRevenueByName.merge(itemName, itemRevenue, Double::sum);
            itemsInReceipt.add(itemName);

            PriceTier tier = PriceTier.fromUnitPrice(item.getUnitPrice());
            quantityByPriceTier.merge(tier, (long) item.getQuantity(), Long::sum);
            revenueByPriceTier.merge(tier, itemRevenue, Double::sum);
        }

        itemAverageAccumulator.addResolved(orderTotal, itemsInReceipt);
        totalAverageAccumulator.addResolved(orderTotal);

        totalRevenue += orderTotal;
        minReceipt = Math.min(minReceipt, orderTotal);
        maxReceipt = Math.max(maxReceipt, orderTotal);

        totalItemsSold += itemsInOrder;

        totalLoyaltyPoints += r.getLoyaltyPointsEarned();

        ordersByStatus.merge(r.getStatus(), 1L, Long::sum);
        revenueByStatus.merge(r.getStatus(), orderTotal, Double::sum);

        revenueByMonth.merge(r.getDate().getMonthValue(), orderTotal, Double::sum);

        String customerKey = r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName();
        uniqueCustomers.add(customerKey);
        revenueByCustomer.merge(customerKey, orderTotal, Double::sum);
        ordersByCustomer.merge(customerKey, 1L, Long::sum);

        String city = r.getShippingAddress().city();
        revenueByCity.merge(city, orderTotal, Double::sum);
        ordersByCity.merge(city, 1L, Long::sum);

        String state = r.getShippingAddress().state();
        revenueByState.merge(state, orderTotal, Double::sum);
        ordersByState.merge(state, 1L, Long::sum);
    }

    ReceiptStatisticsAccumulator combine(ReceiptStatisticsAccumulator other) {
        totalOrders += other.totalOrders;
        totalRevenue += other.totalRevenue;
        minReceipt = Math.min(minReceipt, other.minReceipt);
        maxReceipt = Math.max(maxReceipt, other.maxReceipt);

        totalItemsSold += other.totalItemsSold;
        totalLoyaltyPoints += other.totalLoyaltyPoints;

        other.ordersByStatus.forEach((k, v) -> ordersByStatus.merge(k, v, Long::sum));

        other.revenueByMonth.forEach((k, v) -> revenueByMonth.merge(k, v, Double::sum));

        uniqueCustomers.addAll(other.uniqueCustomers);

        other.revenueByCustomer.forEach((k, v) -> revenueByCustomer.merge(k, v, Double::sum));
        other.ordersByCustomer.forEach((k, v) -> ordersByCustomer.merge(k, v, Long::sum));
        other.itemQuantityByName.forEach((k, v) -> itemQuantityByName.merge(k, v, Long::sum));
        other.itemRevenueByName.forEach((k, v) -> itemRevenueByName.merge(k, v, Double::sum));
        other.revenueByCity.forEach((k, v) -> revenueByCity.merge(k, v, Double::sum));
        other.ordersByCity.forEach((k, v) -> ordersByCity.merge(k, v, Long::sum));
        other.revenueByStatus.forEach((k, v) -> revenueByStatus.merge(k, v, Double::sum));
        other.quantityByPriceTier.forEach((k, v) -> quantityByPriceTier.merge(k, v, Long::sum));
        other.revenueByPriceTier.forEach((k, v) -> revenueByPriceTier.merge(k, v, Double::sum));
        other.revenueByState.forEach((k, v) -> revenueByState.merge(k, v, Double::sum));
        other.ordersByState.forEach((k, v) -> ordersByState.merge(k, v, Long::sum));
        itemAverageAccumulator.combine(other.itemAverageAccumulator);
        totalAverageAccumulator.combine(other.totalAverageAccumulator);

        return this;
    }

    ReceiptStatistics toStatistics() {
        ReceiptStatistics stats = new ReceiptStatistics();
        stats.setTotalOrders(totalOrders);
        stats.setTotalRevenue(totalRevenue);
        stats.setAverageReceiptAmount(totalOrders > 0 ? totalRevenue / totalOrders : 0);
        stats.setMinReceiptAmount(totalOrders > 0 ? minReceipt : 0);
        stats.setMaxReceiptAmount(totalOrders > 0 ? maxReceipt : 0);

        stats.setOrdersByStatus(ordersByStatus);
        stats.setTotalItemsSold(totalItemsSold);

        stats.setUniqueCustomers(uniqueCustomers.size());

        stats.setTotalLoyaltyPoints(totalLoyaltyPoints);
        stats.setRevenueByMonth(revenueByMonth);
        stats.setTopCustomersBySpending(TopMetrics.calculateTopCustomers(revenueByCustomer));
        stats.setTopCustomersByOrderCount(
                TopMetrics.calculateTopCustomersByOrders(ordersByCustomer, revenueByCustomer));
        stats.setTopItemsByQuantity(TopMetrics.calculateTopItems(itemQuantityByName, itemRevenueByName));
        stats.setItemAverageReceipts(itemAverageAccumulator.finish());
        stats.setTopCitiesByRevenue(TopMetrics.calculateTopCities(revenueByCity, ordersByCity));
        stats.setRevenueByStatusRanking(TopMetrics.calculateStatusRevenue(revenueByStatus, ordersByStatus));
        stats.setSalesByPriceTier(TopMetrics.calculatePriceTierSales(quantityByPriceTier, revenueByPriceTier));
        stats.setTopStatesByRevenue(TopMetrics.calculateTopStates(revenueByState, ordersByState));

        return stats;
    }

    private String resolveItemName(Item item) {
        return itemNameDelayMillis > 0 ? item.getName(itemNameDelayMillis) : item.getName();
    }
}
