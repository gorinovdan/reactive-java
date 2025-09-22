package rj.lab1.statistics;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import lombok.Getter;
import lombok.Setter;
import rj.lab1.model.ReceiptStatus;

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
    private List<CustomerSpending> topCustomersBySpending;
    private List<CustomerOrderProfile> topCustomersByOrderCount;
    private List<ItemSales> topItemsByQuantity;
    private List<ItemAverageReceipt> itemAverageReceipts;
    private List<CityRevenue> topCitiesByRevenue;
    private List<StatusRevenue> revenueByStatusRanking;
    private List<PriceTierSales> salesByPriceTier;
    private List<StateRevenue> topStatesByRevenue;

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
        joiner.add("topCustomersBySpending=" + topCustomersBySpending);
        joiner.add("topCustomersByOrderCount=" + topCustomersByOrderCount);
        joiner.add("topItemsByQuantity=" + topItemsByQuantity);
        joiner.add("itemAverageReceipts=" + itemAverageReceipts);
        joiner.add("topCitiesByRevenue=" + topCitiesByRevenue);
        joiner.add("revenueByStatusRanking=" + revenueByStatusRanking);
        joiner.add("salesByPriceTier=" + salesByPriceTier);
        joiner.add("topStatesByRevenue=" + topStatesByRevenue);
        return joiner.toString();
    }
}
