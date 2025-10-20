package lab3.statistics.model;

import java.util.*;

import lab3.model.ReceiptStatus;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class ReceiptStatistics {

    private double totalRevenue;
    private double averageReceiptAmount;
    private TotalAverage totalAverage = TotalAverage.empty();
    private double minReceiptAmount;
    private double maxReceiptAmount;

    private long totalOrders;
    private Map<ReceiptStatus, Long> ordersByStatus = new EnumMap<>(ReceiptStatus.class);

    private long totalItemsSold;
    private long uniqueCustomers;
    private long totalLoyaltyPoints;
    private Map<Integer, Double> revenueByMonth = new HashMap<>();
    private List<CustomerSpending> topCustomersBySpending = new ArrayList<>();
    private List<CustomerOrderProfile> topCustomersByOrderCount = new ArrayList<>();
    private List<ItemSales> topItemsByQuantity = new ArrayList<>();
    private List<ItemAverageReceipt> itemAverageReceipts = new ArrayList<>();
    private List<CityRevenue> topCitiesByRevenue = new ArrayList<>();
    private List<StatusRevenue> revenueByStatusRanking = new ArrayList<>();
    private List<PriceTierSales> salesByPriceTier = new ArrayList<>();
    private List<StateRevenue> topStatesByRevenue = new ArrayList<>();

    /**
     * Объединяет текущую статистику с другой.
     * Возвращает this для удобства chain-вызовов.
     */
    public ReceiptStatistics merge(ReceiptStatistics other) {
        if (other == null) return this;

        // --- числовые поля ---
        this.totalRevenue += other.totalRevenue;
        this.totalOrders += other.totalOrders;
        this.totalItemsSold += other.totalItemsSold;
        this.totalLoyaltyPoints += other.totalLoyaltyPoints;
        this.uniqueCustomers += other.uniqueCustomers;

        this.minReceiptAmount = combineMin(this.minReceiptAmount, other.minReceiptAmount);
        this.maxReceiptAmount = combineMax(this.maxReceiptAmount, other.maxReceiptAmount);
        double average = this.totalOrders > 0
                ? this.totalRevenue / this.totalOrders
                : 0;
        this.totalAverage = new TotalAverage(this.totalOrders, this.totalRevenue, average);
        this.averageReceiptAmount = average;

        mergeMapLong(this.ordersByStatus, other.ordersByStatus);
        mergeMapDouble(this.revenueByMonth, other.revenueByMonth);

        this.topCustomersBySpending = mergeLists(this.topCustomersBySpending, other.topCustomersBySpending);
        this.topCustomersByOrderCount = mergeLists(this.topCustomersByOrderCount, other.topCustomersByOrderCount);
        this.topItemsByQuantity = mergeLists(this.topItemsByQuantity, other.topItemsByQuantity);
        this.itemAverageReceipts = mergeLists(this.itemAverageReceipts, other.itemAverageReceipts);
        this.topCitiesByRevenue = mergeLists(this.topCitiesByRevenue, other.topCitiesByRevenue);
        this.revenueByStatusRanking = mergeLists(this.revenueByStatusRanking, other.revenueByStatusRanking);
        this.salesByPriceTier = mergeLists(this.salesByPriceTier, other.salesByPriceTier);
        this.topStatesByRevenue = mergeLists(this.topStatesByRevenue, other.topStatesByRevenue);

        return this;
    }

    private double combineMin(double a, double b) {
        if (a == 0) return b;
        if (b == 0) return a;
        return Math.min(a, b);
    }

    private double combineMax(double a, double b) {
        return Math.max(a, b);
    }

    private void mergeMapLong(Map<ReceiptStatus, Long> base, Map<ReceiptStatus, Long> other) {
        if (other == null) return;
        other.forEach((k, v) -> base.merge(k, v, Long::sum));
    }

    private void mergeMapDouble(Map<Integer, Double> base, Map<Integer, Double> other) {
        if (other == null) return;
        other.forEach((k, v) -> base.merge(k, v, Double::sum));
    }

    private <T> List<T> mergeLists(List<T> a, List<T> b) {
        if ((a == null || a.isEmpty()) && (b == null || b.isEmpty())) return Collections.emptyList();
        if (a == null || a.isEmpty()) return new ArrayList<>(b);
        if (b == null || b.isEmpty()) return new ArrayList<>(a);

        List<T> merged = new ArrayList<>(a.size() + b.size());
        merged.addAll(a);
        merged.addAll(b);
        return merged;
    }
}
