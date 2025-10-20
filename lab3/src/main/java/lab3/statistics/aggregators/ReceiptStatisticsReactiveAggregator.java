package lab3.statistics.aggregators;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lab3.model.Receipt;
import lab3.model.ReceiptStatus;
import lab3.statistics.Collectors.ReceiptStatisticsCollector;
import lab3.statistics.model.ReceiptStatistics;
import lab3.statistics.model.TotalAverage;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ReceiptStatisticsReactiveAggregator {

    private static final int DEFAULT_MIN_BATCH = 256;

    private ReceiptStatisticsReactiveAggregator() {
    }

    public static ReceiptStatistics aggregateReactive(List<Receipt> receipts, long itemNameDelayMillis) {
        int parallelism = Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(parallelism);
        Scheduler scheduler = Schedulers.from(exec);

        try {
            return aggregateReactive(receipts, itemNameDelayMillis, DEFAULT_MIN_BATCH, parallelism, scheduler);
        } finally {
            exec.shutdown();
        }
    }

    public static ReceiptStatistics aggregateReactive(
            List<Receipt> receipts,
            long itemNameDelayMillis,
            int minimumBatchSize,
            int parallelism,
            Scheduler scheduler) {

        if (receipts == null || receipts.isEmpty()) {
            return new ReceiptStatistics();
        }

        return Observable.fromIterable(receipts)
                .buffer(minimumBatchSize)
                .flatMap(
                        batch -> Observable.fromCallable(() ->
                                batch.stream().collect(ReceiptStatisticsCollector.withItemNameDelay(itemNameDelayMillis))
                        ).subscribeOn(scheduler),
                        parallelism
                )
                .reduce(ReceiptStatisticsReactiveAggregator::mergeStatistics)
                .blockingGet();
    }

    /**
     * Слияние двух ReceiptStatistics.
     * Выполняется последовательно, потокобезопасно при вызове внутри reduce().
     */
    private static ReceiptStatistics mergeStatistics(ReceiptStatistics a, ReceiptStatistics b) {
        if (a == null) return b;
        if (b == null) return a;

        ReceiptStatistics merged = new ReceiptStatistics();

        merged.setTotalRevenue(a.getTotalRevenue() + b.getTotalRevenue());
        merged.setTotalOrders(a.getTotalOrders() + b.getTotalOrders());
        merged.setTotalItemsSold(a.getTotalItemsSold() + b.getTotalItemsSold());
        merged.setTotalLoyaltyPoints(a.getTotalLoyaltyPoints() + b.getTotalLoyaltyPoints());
        merged.setUniqueCustomers(a.getUniqueCustomers() + b.getUniqueCustomers());

        merged.setMinReceiptAmount(
                Math.min(nonZeroOrMax(a.getMinReceiptAmount()), nonZeroOrMax(b.getMinReceiptAmount()))
        );
        merged.setMaxReceiptAmount(
                Math.max(a.getMaxReceiptAmount(), b.getMaxReceiptAmount())
        );

        double totalOrders = merged.getTotalOrders();
        double average = totalOrders > 0
                ? merged.getTotalRevenue() / totalOrders
                : 0;
        merged.setTotalAverage(new TotalAverage(totalOrders, merged.getTotalRevenue(), average));
        merged.setAverageReceiptAmount(average);

        // Объединяем карты и списки (простое объединение; можно оптимизировать под TopN)
        merged.setOrdersByStatus(mergeMapLong(a.getOrdersByStatus(), b.getOrdersByStatus()));
        merged.setRevenueByMonth(mergeMapDouble(a.getRevenueByMonth(), b.getRevenueByMonth()));

        merged.setTopCustomersBySpending(mergeLists(a.getTopCustomersBySpending(), b.getTopCustomersBySpending()));
        merged.setTopCustomersByOrderCount(mergeLists(a.getTopCustomersByOrderCount(), b.getTopCustomersByOrderCount()));
        merged.setTopItemsByQuantity(mergeLists(a.getTopItemsByQuantity(), b.getTopItemsByQuantity()));
        merged.setItemAverageReceipts(mergeLists(a.getItemAverageReceipts(), b.getItemAverageReceipts()));
        merged.setTopCitiesByRevenue(mergeLists(a.getTopCitiesByRevenue(), b.getTopCitiesByRevenue()));
        merged.setRevenueByStatusRanking(mergeLists(a.getRevenueByStatusRanking(), b.getRevenueByStatusRanking()));
        merged.setSalesByPriceTier(mergeLists(a.getSalesByPriceTier(), b.getSalesByPriceTier()));
        merged.setTopStatesByRevenue(mergeLists(a.getTopStatesByRevenue(), b.getTopStatesByRevenue()));

        return merged;
    }

    private static double nonZeroOrMax(double value) {
        return value == 0 ? Double.MAX_VALUE : value;
    }

    private static Map<ReceiptStatus, Long> mergeMapLong(Map<ReceiptStatus, Long> a, Map<ReceiptStatus, Long> b) {
        Map<ReceiptStatus, Long> result = new EnumMap<>(ReceiptStatus.class);
        if (a != null) a.forEach((k, v) -> result.merge(k, v, Long::sum));
        if (b != null) b.forEach((k, v) -> result.merge(k, v, Long::sum));
        return result;
    }

    private static Map<Integer, Double> mergeMapDouble(Map<Integer, Double> a, Map<Integer, Double> b) {
        Map<Integer, Double> result = new HashMap<>();
        if (a != null) a.forEach((k, v) -> result.merge(k, v, Double::sum));
        if (b != null) b.forEach((k, v) -> result.merge(k, v, Double::sum));
        return result;
    }

    private static <T> List<T> mergeLists(List<T> a, List<T> b) {
        if (a == null && b == null) return Collections.emptyList();
        if (a == null) return new ArrayList<>(b);
        if (b == null) return new ArrayList<>(a);
        List<T> merged = new ArrayList<>(a.size() + b.size());
        merged.addAll(a);
        merged.addAll(b);
        return merged;
    }
}

