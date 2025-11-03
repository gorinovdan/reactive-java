package rj.lab2.statistics.aggregators.top_items_by_quantity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import rj.lab2.model.Item;
import rj.lab2.model.Receipt;
import rj.lab2.statistics.ItemSales;

public final class TopItemsByQuantityAggregator {

    private TopItemsByQuantityAggregator() {
    }

    public static List<ItemSales> topItemsByQuantityCircleAggregator(List<Receipt> receipts) {
        return topItemsByQuantityCircleAggregator(receipts, 0L);
    }

    public static List<ItemSales> topItemsByQuantityCircleAggregator(List<Receipt> receipts, long itemNameDelayMillis) {
        Map<String, double[]> aggregated = new HashMap<>();

        for (Receipt receipt : receipts) {
            for (Item item : receipt.getItems()) {
                String itemName = resolveName(item, itemNameDelayMillis);
                if (itemName == null) {
                    continue;
                }
                double[] bucket = aggregated.computeIfAbsent(itemName, key -> new double[2]);
                bucket[0] += item.getQuantity();
                bucket[1] += item.getQuantity() * item.getUnitPrice();
            }
        }

        List<ItemSales> itemSalesList = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : aggregated.entrySet()) {
            double[] values = entry.getValue();
            itemSalesList.add(new ItemSales(entry.getKey(), (long) values[0], values[1]));
        }
        itemSalesList.sort(ItemSales.byQuantityAndRevenueDescending());
        return itemSalesList;
    }

    public static List<ItemSales> topItemsByQuantityStreamAggregator(List<Receipt> receipts) {
        return topItemsByQuantityStreamAggregator(receipts, 0L);
    }

    public static List<ItemSales> topItemsByQuantityStreamAggregator(List<Receipt> receipts, long itemNameDelayMillis) {
        return receipts.stream()
                .flatMap(receipt -> receipt.getItems().stream()
                        .map(item -> new ItemSnapshot(
                                resolveName(item, itemNameDelayMillis),
                                item.getQuantity(),
                                item.getQuantity() * item.getUnitPrice())))
                .filter(snapshot -> snapshot.name() != null)
                .collect(Collectors.groupingBy(
                        ItemSnapshot::name,
                        Collectors.reducing(
                                new double[2],
                                snapshot -> new double[] { snapshot.quantity(), snapshot.revenue() },
                                (left, right) -> {
                                    left[0] += right[0];
                                    left[1] += right[1];
                                    return left;
                                })))
                .entrySet().stream()
                .map(entry -> new ItemSales(entry.getKey(), (long) entry.getValue()[0], entry.getValue()[1]))
                .sorted(ItemSales.byQuantityAndRevenueDescending())
                .toList();
    }

    public static List<ItemSales> topItemsByQuantity(List<Receipt> receipts) {
        return receipts.stream().collect(new ItemSalesCollector(0L));
    }

    public static List<ItemSales> topItemsByQuantity(List<Receipt> receipts, long itemNameDelayMillis) {
        return receipts.stream().collect(new ItemSalesCollector(itemNameDelayMillis));
    }

    private static String resolveName(Item item, long delayMillis) {
        return delayMillis > 0 ? item.getName(delayMillis) : item.getName();
    }

    private record ItemSnapshot(String name, long quantity, double revenue) {
    }

    public static class ItemSalesCollector implements Collector<Receipt, Map<String, double[]>, List<ItemSales>> {

        private final long itemNameDelayMillis;

        public ItemSalesCollector() {
            this(0L);
        }

        public ItemSalesCollector(long itemNameDelayMillis) {
            this.itemNameDelayMillis = Math.max(0L, itemNameDelayMillis);
        }

        @Override
        public Supplier<Map<String, double[]>> supplier() {
            return HashMap::new;
        }

        @Override
        public BiConsumer<Map<String, double[]>, Receipt> accumulator() {
            return (acc, receipt) -> {
                for (Item item : receipt.getItems()) {
                    String itemName = resolveName(item, itemNameDelayMillis);
                    if (itemName == null) {
                        continue;
                    }
                    double[] bucket = acc.computeIfAbsent(itemName, key -> new double[2]);
                    bucket[0] += item.getQuantity();
                    bucket[1] += item.getQuantity() * item.getUnitPrice();
                }
            };
        }

        @Override
        public BinaryOperator<Map<String, double[]>> combiner() {
            return (left, right) -> {
                right.forEach((name, stats) -> {
                    double[] bucket = left.computeIfAbsent(name, key -> new double[2]);
                    bucket[0] += stats[0];
                    bucket[1] += stats[1];
                });
                return left;
            };
        }

        @Override
        public Function<Map<String, double[]>, List<ItemSales>> finisher() {
            return map -> map.entrySet().stream()
                    .map(entry -> new ItemSales(entry.getKey(), (long) entry.getValue()[0], entry.getValue()[1]))
                    .sorted(ItemSales.byQuantityAndRevenueDescending())
                    .toList();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }
}
