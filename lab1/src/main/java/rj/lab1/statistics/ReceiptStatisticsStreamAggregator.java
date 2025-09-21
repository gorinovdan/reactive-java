package rj.lab1.statistics;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.model.ReceiptStatus;

import java.util.*;
import java.util.stream.Collectors;

public class ReceiptStatisticsStreamAggregator {

    public static ReceiptStatistics aggregate(List<Receipt> receipts) {
        ReceiptStatistics stats = new ReceiptStatistics();

        // сумма по чекам
        List<Double> orderTotals = receipts.stream()
                .map(r -> r.getItems().stream()
                        .mapToDouble(Item::getTotalPrice)
                        .sum())
                .toList();

        double totalRevenue = orderTotals.stream().mapToDouble(Double::doubleValue).sum();
        long totalOrders = receipts.size();
        double minReceipt = orderTotals.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxReceipt = orderTotals.stream().mapToDouble(Double::doubleValue).max().orElse(0);

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
                .collect(Collectors.groupingBy(Receipt::getStatus, Collectors.counting()));

        // выручка по месяцам
        Map<Integer, Double> revenueByMonth = receipts.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDate().getMonthValue(),
                        Collectors.summingDouble(r -> r.getItems().stream()
                                .mapToDouble(Item::getTotalPrice)
                                .sum())
                ));

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

