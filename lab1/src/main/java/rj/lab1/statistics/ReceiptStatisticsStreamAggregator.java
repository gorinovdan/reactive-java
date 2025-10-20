package rj.lab1.statistics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.Set;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;

public class ReceiptStatisticsStreamAggregator {

        public static ReceiptStatistics aggregate(List<Receipt> receipts) {
                return receipts.stream()
                                .collect(Collector.of(
                                                StreamAccumulator::new,
                                                StreamAccumulator::add,
                                                StreamAccumulator::combine,
                                                StreamAccumulator::toStatistics));
        }

        private static final class StreamAccumulator {
                private long totalOrders = 0;
                private double totalRevenue = 0;
                private double minReceipt = Double.POSITIVE_INFINITY;
                private double maxReceipt = Double.NEGATIVE_INFINITY;

                private long totalItemsSold = 0;
                private long totalLoyaltyPoints = 0;

                private final Map<ReceiptStatus, Long> ordersByStatus = new EnumMap<>(ReceiptStatus.class);
                private final Map<Integer, Double> revenueByMonth = new HashMap<>();
                private final Set<String> uniqueCustomers = new HashSet<>();
                private final Map<String, Double> revenueByCustomer = new HashMap<>();
                private final Map<String, Long> ordersByCustomer = new HashMap<>();
                private final Map<String, Long> itemQuantityByName = new HashMap<>();
                private final Map<String, Double> itemRevenueByName = new HashMap<>();
                private final Map<String, Double> revenueByCity = new HashMap<>();
                private final Map<String, Long> ordersByCity = new HashMap<>();
                private final Map<ReceiptStatus, Double> revenueByStatus = new EnumMap<>(ReceiptStatus.class);
                private final Map<PriceTier, Long> quantityByPriceTier = new EnumMap<>(PriceTier.class);
                private final Map<PriceTier, Double> revenueByPriceTier = new EnumMap<>(PriceTier.class);
                private final Map<String, Double> revenueByState = new HashMap<>();
                private final Map<String, Long> ordersByState = new HashMap<>();
                private final ItemAverageReceiptMetrics.ItemAverageAccumulator itemAverageAccumulator =
                                ItemAverageReceiptMetrics.newAccumulator();
                private final TotalAverageMetrics.TotalAverageAccumulator totalAverageAccumulator =
                                TotalAverageMetrics.newAccumulator();

                void add(Receipt receipt) {
                        totalOrders++;

                        double orderTotal = 0;
                        long itemsInOrder = 0;
                        Set<String> itemsInReceipt = new HashSet<>();

                        for (Item item : receipt.getItems()) {
                                double itemRevenue = item.getUnitPrice() * item.getQuantity();
                                orderTotal += itemRevenue;
                                itemsInOrder += item.getQuantity();

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

                        totalItemsSold += itemsInOrder;

                        totalLoyaltyPoints += receipt.getLoyaltyPointsEarned();

                        ordersByStatus.merge(receipt.getStatus(), 1L, Long::sum);
                        revenueByStatus.merge(receipt.getStatus(), orderTotal, Double::sum);

                        revenueByMonth.merge(receipt.getDate().getMonthValue(), orderTotal, Double::sum);

                        String customerKey = receipt.getCustomer().getFirstName() + " " + receipt.getCustomer().getLastName();
                        uniqueCustomers.add(customerKey);
                        revenueByCustomer.merge(customerKey, orderTotal, Double::sum);
                        ordersByCustomer.merge(customerKey, 1L, Long::sum);

                        String city = receipt.getShippingAddress().city();
                        revenueByCity.merge(city, orderTotal, Double::sum);
                        ordersByCity.merge(city, 1L, Long::sum);

                        String state = receipt.getShippingAddress().state();
                        revenueByState.merge(state, orderTotal, Double::sum);
                        ordersByState.merge(state, 1L, Long::sum);
                }

                StreamAccumulator combine(StreamAccumulator other) {
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
                        stats.setRevenueByStatusRanking(
                                        TopMetrics.calculateStatusRevenue(revenueByStatus, ordersByStatus));
                        stats.setSalesByPriceTier(TopMetrics.calculatePriceTierSales(quantityByPriceTier, revenueByPriceTier));
                        stats.setTopStatesByRevenue(TopMetrics.calculateTopStates(revenueByState, ordersByState));

                        return stats;
                }
        }
}
