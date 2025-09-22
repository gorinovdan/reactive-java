package rj.lab1.statistics.Collectors;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;
import rj.lab1.statistics.ReceiptStatistics;

public class ReceiptStatisticsAccumulator {
    long totalOrders = 0;
    double totalRevenue = 0;
    double minReceipt = Double.POSITIVE_INFINITY;
    double maxReceipt = Double.NEGATIVE_INFINITY;

    long totalItemsSold = 0;
    long totalLoyaltyPoints = 0;

    Map<ReceiptStatus, Long> ordersByStatus = new EnumMap<>(ReceiptStatus.class);
    Map<Integer, Double> revenueByMonth = new HashMap<>();
    Set<String> uniqueCustomers = new HashSet<>();

    void add(Receipt r) {
        totalOrders++;

        double orderTotal = 0;
        long itemsInOrder = 0;
        for (Item item : r.getItems()) {
            orderTotal += item.getTotalPrice();
            itemsInOrder += item.getQuantity();
        }

        totalRevenue += orderTotal;
        minReceipt = Math.min(minReceipt, orderTotal);
        maxReceipt = Math.max(maxReceipt, orderTotal);

        totalItemsSold += itemsInOrder;

        totalLoyaltyPoints += r.getLoyaltyPointsEarned();

        ordersByStatus.merge(r.getStatus(), 1L, Long::sum);

        revenueByMonth.merge(r.getDate().getMonthValue(), orderTotal, Double::sum);

        uniqueCustomers.add(r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName());
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

        return stats;
    }
}
