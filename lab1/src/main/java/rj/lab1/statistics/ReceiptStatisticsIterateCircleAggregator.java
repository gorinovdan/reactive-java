package rj.lab1.statistics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;

public class ReceiptStatisticsIterateCircleAggregator {

    public static ReceiptStatistics aggregate(List<Receipt> receipts) {
        ReceiptStatistics stats = new ReceiptStatistics();

        // агрегаты
        double totalRevenue = 0;
        double minReceipt = Double.POSITIVE_INFINITY;
        double maxReceipt = Double.NEGATIVE_INFINITY;

        long totalOrders = 0;
        long totalItemsSold = 0;
        long totalLoyaltyPoints = 0;

        Map<ReceiptStatus, Long> ordersByStatus = new EnumMap<>(ReceiptStatus.class);
        Map<Integer, Double> revenueByMonth = new HashMap<>();
        Set<String> uniqueCustomers = new HashSet<>();

        for (Receipt r : receipts) {
            totalOrders++;

            double orderTotal = 0;
            for (Item item : r.getItems()) {
                totalItemsSold += item.getQuantity();
                orderTotal += item.getTotalPrice();
            }

            totalRevenue += orderTotal;
            minReceipt = Math.min(minReceipt, orderTotal);
            maxReceipt = Math.max(maxReceipt, orderTotal);

            ordersByStatus.merge(r.getStatus(), 1L, Long::sum);

            String customerKey = r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName();
            uniqueCustomers.add(customerKey);

            int month = r.getDate().getMonthValue();
            revenueByMonth.merge(month, orderTotal, Double::sum);

            totalLoyaltyPoints += r.getLoyaltyPointsEarned();
        }

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
