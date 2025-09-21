package rj.lab1.statistics.Collectors;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;
import rj.lab1.statistics.ReceiptStatistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReceiptStatisticsAccumulator {
    long totalOrders = 0;
    double totalRevenue = 0;
    double minReceipt = Double.MAX_VALUE;
    double maxReceipt = Double.MIN_VALUE;

    long totalItemsSold = 0;
    long totalLoyaltyPoints = 0;

    Map<ReceiptStatus, Long> ordersByStatus = new HashMap<>();
    Map<Integer, Double> revenueByMonth = new HashMap<>();
    Set<String> uniqueCustomers = new HashSet<>();

    void add(Receipt r) {
        totalOrders++;

        double orderTotal = r.getItems().stream()
                .mapToDouble(Item::getTotalPrice)
                .sum();

        totalRevenue += orderTotal;
        minReceipt = Math.min(minReceipt, orderTotal);
        maxReceipt = Math.max(maxReceipt, orderTotal);

        totalItemsSold += r.getItems().stream()
                .mapToLong(Item::getQuantity)
                .sum();

        totalLoyaltyPoints += r.getLoyaltyPointsEarned();

        ordersByStatus.merge(r.getStatus(), 1L, Long::sum);

        revenueByMonth.merge(r.getDate().getMonthValue(), orderTotal, Double::sum);

        uniqueCustomers.add(r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName());
    }

    ReceiptStatisticsAccumulator combine(ReceiptStatisticsAccumulator other) {
        ReceiptStatisticsAccumulator acc = new ReceiptStatisticsAccumulator();

        acc.totalOrders = this.totalOrders + other.totalOrders;
        acc.totalRevenue = this.totalRevenue + other.totalRevenue;
        acc.minReceipt = Math.min(this.minReceipt, other.minReceipt);
        acc.maxReceipt = Math.max(this.maxReceipt, other.maxReceipt);

        acc.totalItemsSold = this.totalItemsSold + other.totalItemsSold;
        acc.totalLoyaltyPoints = this.totalLoyaltyPoints + other.totalLoyaltyPoints;

        acc.ordersByStatus = new HashMap<>(this.ordersByStatus);
        other.ordersByStatus.forEach((k, v) -> acc.ordersByStatus.merge(k, v, Long::sum));

        acc.revenueByMonth = new HashMap<>(this.revenueByMonth);
        other.revenueByMonth.forEach((k, v) -> acc.revenueByMonth.merge(k, v, Double::sum));

        acc.uniqueCustomers = new HashSet<>(this.uniqueCustomers);
        acc.uniqueCustomers.addAll(other.uniqueCustomers);

        return acc;
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
