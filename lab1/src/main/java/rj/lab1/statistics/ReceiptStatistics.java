package rj.lab1.statistics;

import lombok.Getter;
import lombok.Setter;
import rj.lab1.model.ReceiptStatus;

import java.util.Map;
import java.util.StringJoiner;

@Setter
@Getter
public class ReceiptStatistics {
    private double totalRevenue;
    private double averageReceiptAmount;
    private double minReceiptAmount;
    private double maxReceiptAmount;

    private long totalOrders;
    private Map<ReceiptStatus, Long> ordersByStatus;

    private long totalItemsSold;
    private long uniqueCustomers;
    private long totalLoyaltyPoints;
    private Map<Integer, Double> revenueByMonth;

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner("\n  ", "ReceiptStatistics{\n  ", "\n}");
        joiner.add("totalRevenue=" + totalRevenue);
        joiner.add("averageReceiptAmount=" + averageReceiptAmount);
        joiner.add("minReceiptAmount=" + minReceiptAmount);
        joiner.add("maxReceiptAmount=" + maxReceiptAmount);
        joiner.add("totalOrders=" + totalOrders);
        joiner.add("ordersByStatus=" + ordersByStatus);
        joiner.add("totalItemsSold=" + totalItemsSold);
        joiner.add("uniqueCustomers=" + uniqueCustomers);
        joiner.add("totalLoyaltyPoints=" + totalLoyaltyPoints);
        joiner.add("revenueByMonth=" + revenueByMonth);
        return joiner.toString();
    }
}

