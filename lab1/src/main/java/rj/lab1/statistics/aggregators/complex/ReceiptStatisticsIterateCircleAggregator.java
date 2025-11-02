package rj.lab1.statistics.aggregators.complex;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;
import rj.lab1.statistics.*;
import rj.lab1.statistics.metrics.ItemAverageReceiptMetrics;

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
        Map<String, Double> revenueByCustomer = new HashMap<>();
        Map<String, Long> ordersByCustomer = new HashMap<>();
        Map<String, Long> itemQuantityByName = new HashMap<>();
        Map<String, Double> itemRevenueByName = new HashMap<>();
        Map<String, Double> revenueByCity = new HashMap<>();
        Map<String, Long> ordersByCity = new HashMap<>();
        Map<ReceiptStatus, Double> revenueByStatus = new EnumMap<>(ReceiptStatus.class);
        Map<PriceTier, Long> quantityByPriceTier = new EnumMap<>(PriceTier.class);
        Map<PriceTier, Double> revenueByPriceTier = new EnumMap<>(PriceTier.class);
        Map<String, Double> revenueByState = new HashMap<>();
        Map<String, Long> ordersByState = new HashMap<>();
        ItemAverageReceiptMetrics.ItemAverageAccumulator itemAverageAccumulator =
                ItemAverageReceiptMetrics.newAccumulator();
        TotalAverageMetrics.TotalAverageAccumulator totalAverageAccumulator =
                TotalAverageMetrics.newAccumulator();

        for (Receipt r : receipts) {
            totalOrders++;

            double orderTotal = 0;
            Set<String> itemsInReceipt = new HashSet<>();
            for (Item item : r.getItems()) {
                totalItemsSold += item.getQuantity();
                double itemRevenue = item.getUnitPrice() * item.getQuantity();
                orderTotal += itemRevenue;
                String itemName = item.getName();
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

            ordersByStatus.merge(r.getStatus(), 1L, Long::sum);

            String customerKey = r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName();
            uniqueCustomers.add(customerKey);
            revenueByCustomer.merge(customerKey, orderTotal, Double::sum);
            ordersByCustomer.merge(customerKey, 1L, Long::sum);

            int month = r.getDate().getMonthValue();
            revenueByMonth.merge(month, orderTotal, Double::sum);

            totalLoyaltyPoints += r.getLoyaltyPointsEarned();

            String city = r.getShippingAddress().city();
            revenueByCity.merge(city, orderTotal, Double::sum);
            ordersByCity.merge(city, 1L, Long::sum);

            revenueByStatus.merge(r.getStatus(), orderTotal, Double::sum);

            String state = r.getShippingAddress().state();
            revenueByState.merge(state, orderTotal, Double::sum);
            ordersByState.merge(state, 1L, Long::sum);
        }

        stats.setTotalOrders(totalOrders);
        stats.setTotalRevenue(totalRevenue);
        TotalAverage totalAverage = totalAverageAccumulator.finish();
        stats.setTotalAverage(totalAverage);
        stats.setAverageReceiptAmount(totalAverage.averageReceiptAmount());
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
}
