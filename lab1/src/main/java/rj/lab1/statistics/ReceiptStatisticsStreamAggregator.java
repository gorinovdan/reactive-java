package rj.lab1.statistics;

import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;

public class ReceiptStatisticsStreamAggregator {

        public static ReceiptStatistics aggregate(List<Receipt> receipts) {
                ReceiptStatistics stats = new ReceiptStatistics();

                // сумма по чекам
                DoubleSummaryStatistics orderSummary = receipts.stream()
                                .mapToDouble(r -> r.getItems().stream()
                                                .mapToDouble(Item::getTotalPrice)
                                                .sum())
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
                                                Collectors.summingDouble(r -> r.getItems().stream()
                                                                .mapToDouble(Item::getTotalPrice)
                                                                .sum())));

                // уникальные клиенты
                long uniqueCustomers = receipts.stream()
                                .map(r -> r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName())
                                .collect(Collectors.toSet())
                                .size();

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

                return stats;
        }
}
