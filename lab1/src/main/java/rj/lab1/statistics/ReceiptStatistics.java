package rj.lab1.statistics;

import lombok.Getter;
import lombok.Setter;
import rj.lab1.model.ReceiptStatus;

import java.util.Map;

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
    private Map<String, Long> itemsByQuantity;
    private Map<String, Double> itemsByRevenue;

    private long uniqueCustomers;
    private Map<String, Double> revenueByCustomer;

    private long totalLoyaltyPoints;
    private Map<Integer, Double> revenueByMonth;
}

