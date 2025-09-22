package rj.lab1.statistics;

import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;

public class ReceiptStatisticsStreamAggregator {

        public static ReceiptStatistics aggregate(List<Receipt> receipts) {
                ReceiptStatistics stats = new ReceiptStatistics();

                // сумма по чекам
                DoubleSummaryStatistics orderSummary = receipts.stream()
                                .mapToDouble(ReceiptStatisticsStreamAggregator::orderTotal)
                                .summaryStatistics();

                double totalRevenue = orderSummary.getSum();
                long totalOrders = orderSummary.getCount();
                double minReceipt = totalOrders > 0 ? orderSummary.getMin() : 0;
                double maxReceipt = totalOrders > 0 ? orderSummary.getMax() : 0;

                // количество проданных товаров
                long totalItemsSold = receipts.stream()
                                .flatMap(r -> r.getItems().stream())
                                .mapToLong(Item::getQuantity)
                                .sum();

                // сумма бонусных баллов
                long totalLoyaltyPoints = receipts.stream()
                                .mapToLong(Receipt::getLoyaltyPointsEarned)
                                .sum();

                // заказы по статусам
                Map<ReceiptStatus, Long> ordersByStatus = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                Receipt::getStatus,
                                                () -> new EnumMap<>(ReceiptStatus.class),
                                                Collectors.counting()));

                // выручка по месяцам
                Map<Integer, Double> revenueByMonth = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                r -> r.getDate().getMonthValue(),
                                                Collectors.summingDouble(
                                                                ReceiptStatisticsStreamAggregator::orderTotal)));

                // уникальные клиенты
                long uniqueCustomers = receipts.stream()
                                .map(ReceiptStatisticsStreamAggregator::customerName)
                                .collect(Collectors.toSet())
                                .size();

                Map<String, Double> revenueByCustomer = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                ReceiptStatisticsStreamAggregator::customerName,
                                                Collectors.summingDouble(
                                                                ReceiptStatisticsStreamAggregator::orderTotal)));

                Map<String, Long> ordersByCustomer = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                ReceiptStatisticsStreamAggregator::customerName,
                                                Collectors.counting()));

                Map<String, Long> itemQuantityByName = receipts.stream()
                                .flatMap(r -> r.getItems().stream())
                                .collect(Collectors.groupingBy(
                                                Item::getName,
                                                Collectors.summingLong(Item::getQuantity)));

                Map<String, Double> itemRevenueByName = receipts.stream()
                                .flatMap(r -> r.getItems().stream())
                                .collect(Collectors.groupingBy(
                                                Item::getName,
                                                Collectors.summingDouble(
                                                                ReceiptStatisticsStreamAggregator::itemRevenue)));

                Map<String, Long> itemReceiptCountByName = receipts.stream()
                                .flatMap(r -> r.getItems().stream()
                                                .map(Item::getName)
                                                .distinct())
                                .collect(Collectors.groupingBy(
                                                Function.identity(),
                                                Collectors.counting()));

                Map<String, Double> itemReceiptTotalByName = receipts.stream()
                                .flatMap(r -> {
                                        double total = orderTotal(r);
                                        return r.getItems().stream()
                                                        .map(Item::getName)
                                                        .distinct()
                                                        .map(itemName -> Map.entry(itemName, total));
                                })
                                .collect(Collectors.groupingBy(
                                                Map.Entry::getKey,
                                                Collectors.summingDouble(Map.Entry::getValue)));

                Map<String, Double> revenueByCity = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                r -> r.getShippingAddress().city(),
                                                Collectors.summingDouble(
                                                                ReceiptStatisticsStreamAggregator::orderTotal)));

                Map<String, Long> ordersByCity = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                r -> r.getShippingAddress().city(),
                                                Collectors.counting()));

                Map<ReceiptStatus, Double> revenueByStatus = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                Receipt::getStatus,
                                                () -> new EnumMap<>(ReceiptStatus.class),
                                                Collectors.summingDouble(
                                                                ReceiptStatisticsStreamAggregator::orderTotal)));

                Map<PriceTier, Long> quantityByPriceTier = receipts.stream()
                                .flatMap(r -> r.getItems().stream())
                                .collect(Collectors.groupingBy(
                                                item -> PriceTier.fromUnitPrice(item.getUnitPrice()),
                                                () -> new EnumMap<>(PriceTier.class),
                                                Collectors.summingLong(Item::getQuantity)));

                Map<PriceTier, Double> revenueByPriceTier = receipts.stream()
                                .flatMap(r -> r.getItems().stream())
                                .collect(Collectors.groupingBy(
                                                item -> PriceTier.fromUnitPrice(item.getUnitPrice()),
                                                () -> new EnumMap<>(PriceTier.class),
                                                Collectors.summingDouble(
                                                                ReceiptStatisticsStreamAggregator::itemRevenue)));

                Map<String, Double> revenueByState = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                r -> r.getShippingAddress().state(),
                                                Collectors.summingDouble(
                                                                ReceiptStatisticsStreamAggregator::orderTotal)));

                Map<String, Long> ordersByState = receipts.stream()
                                .collect(Collectors.groupingBy(
                                                r -> r.getShippingAddress().state(),
                                                Collectors.counting()));

                // заполнение статистики
                stats.setTotalOrders(totalOrders);
                stats.setTotalRevenue(totalRevenue);
                stats.setAverageReceiptAmount(totalOrders > 0 ? totalRevenue / totalOrders : 0);
                stats.setMinReceiptAmount(minReceipt);
                stats.setMaxReceiptAmount(maxReceipt);

                stats.setOrdersByStatus(ordersByStatus);
                stats.setTotalItemsSold(totalItemsSold);

                stats.setUniqueCustomers(uniqueCustomers);

                stats.setTotalLoyaltyPoints(totalLoyaltyPoints);
                stats.setRevenueByMonth(revenueByMonth);
                stats.setTopCustomersBySpending(TopMetrics.calculateTopCustomers(revenueByCustomer));
                stats.setTopCustomersByOrderCount(
                                TopMetrics.calculateTopCustomersByOrders(ordersByCustomer, revenueByCustomer));
                stats.setTopItemsByQuantity(TopMetrics.calculateTopItems(itemQuantityByName, itemRevenueByName));
                stats.setItemAverageReceipts(
                                TopMetrics.calculateItemAverageReceipts(
                                                itemReceiptCountByName,
                                                itemReceiptTotalByName));
                stats.setTopCitiesByRevenue(TopMetrics.calculateTopCities(revenueByCity, ordersByCity));
                stats.setRevenueByStatusRanking(TopMetrics.calculateStatusRevenue(revenueByStatus, ordersByStatus));
                stats.setSalesByPriceTier(TopMetrics.calculatePriceTierSales(quantityByPriceTier, revenueByPriceTier));
                stats.setTopStatesByRevenue(TopMetrics.calculateTopStates(revenueByState, ordersByState));

                return stats;
        }

        private static String customerName(Receipt receipt) {
                return receipt.getCustomer().getFirstName() + " " + receipt.getCustomer().getLastName();
        }

        private static double orderTotal(Receipt receipt) {
                return receipt.getItems().stream()
                                .mapToDouble(ReceiptStatisticsStreamAggregator::itemRevenue)
                                .sum();
        }

        private static double itemRevenue(Item item) {
                return item.getUnitPrice() * item.getQuantity();
        }
}
