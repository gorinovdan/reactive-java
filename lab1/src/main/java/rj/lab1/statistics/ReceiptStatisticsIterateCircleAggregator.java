package rj.lab1.statistics;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;

import java.util.*;

public class ReceiptStatisticsIterateCircleAggregator {

    public static ReceiptStatistics aggregate(List<Receipt> receipts) {
        ReceiptStatistics stats = new ReceiptStatistics();

        // агрегаты
        double totalRevenue = 0;
        double minReceipt = Double.MAX_VALUE;
        double maxReceipt = Double.MIN_VALUE;

        long totalOrders = 0;
        long totalItemsSold = 0;
        long totalLoyaltyPoints = 0;

        Map<ReceiptStatus, Long> ordersByStatus = new HashMap<>();
        Map<String, Long> itemsByQuantity = new HashMap<>();
        Map<String, Double> itemsByRevenue = new HashMap<>();
        Map<String, Double> revenueByCustomer = new HashMap<>();
        Map<Integer, Double> revenueByMonth = new HashMap<>();
        Set<String> uniqueCustomers = new HashSet<>();

        for (Receipt r : receipts) {
            totalOrders++;

            double orderTotal = 0;
            for (Item item : r.getItems()) {
                totalItemsSold += item.getQuantity();
                orderTotal += item.getTotalPrice();

                itemsByQuantity.merge(item.getName(), (long) item.getQuantity(), Long::sum);
                itemsByRevenue.merge(item.getName(), item.getTotalPrice(), Double::sum);
            }

            totalRevenue += orderTotal;
            minReceipt = Math.min(minReceipt, orderTotal);
            maxReceipt = Math.max(maxReceipt, orderTotal);

            ordersByStatus.merge(r.getStatus(), 1L, Long::sum);

            String customerKey = r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName();
            revenueByCustomer.merge(customerKey, orderTotal, Double::sum);
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

