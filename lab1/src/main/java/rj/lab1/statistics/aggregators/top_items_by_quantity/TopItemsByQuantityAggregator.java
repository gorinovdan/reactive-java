package rj.lab1.statistics.aggregators.top_items_by_quantity;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ItemSales;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class TopItemsByQuantityAggregator {

    public static List<ItemSales> topItemsByQuantityCircleAggregator(List<Receipt> receipts) {
        Map<String, double[]> aggregated = new HashMap<>();

        for (Receipt r : receipts) {
            for (Item i : r.getItems()) {
                double[] acc = aggregated.computeIfAbsent(i.getName(), k -> new double[2]);
                acc[0] += i.getQuantity(); // quantitySold
                acc[1] += i.getQuantity() * i.getUnitPrice(); // totalRevenue
            }
        }

        List<ItemSales> itemSalesList = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : aggregated.entrySet()) {
            String itemName = entry.getKey();
            double[] values = entry.getValue();
            itemSalesList.add(new ItemSales(
                    itemName,
                    (long) values[0],
                    values[1]
            ));
        }

        itemSalesList.sort(ItemSales.byQuantityAndRevenueDescending());

        return itemSalesList;
    }


    public static List<ItemSales> topItemsByQuantityStreamAggregator(List<Receipt> receipts) {
        return receipts.stream()
                .flatMap(r -> r.getItems().stream())
                .collect(Collectors.groupingBy(
                        Item::getName,
                        Collectors.reducing(
                                new double[2],
                                i -> new double[]{i.getQuantity(), i.getQuantity() * i.getUnitPrice()},
                                (a, b) -> { a[0] += b[0]; a[1] += b[1]; return a; }
                        )
                ))
                .entrySet().stream()
                .map(e -> new ItemSales(e.getKey(), (long) e.getValue()[0], e.getValue()[1]))
                .sorted(ItemSales.byQuantityAndRevenueDescending())
                .toList();
    }


    public static List<ItemSales> topItemsByQuantity(List<Receipt> receipts) {
        return receipts.stream().collect(new ItemSalesCollector());
    }

    public static class ItemSalesCollector implements Collector<Receipt, Map<String, double[]>, List<ItemSales>> {

        @Override
        public Supplier<Map<String, double[]>> supplier() {
            return HashMap::new;
        }

        @Override
        public BiConsumer<Map<String, double[]>, Receipt> accumulator() {
            return (acc, receipt) -> {
                for (Item i : receipt.getItems()) {
                    double[] arr = acc.computeIfAbsent(i.getName(), k -> new double[2]);
                    arr[0] += i.getQuantity(); // quantitySold
                    arr[1] += i.getQuantity() * i.getUnitPrice(); // totalRevenue
                }
            };
        }

        @Override
        public BinaryOperator<Map<String, double[]>> combiner() {
            return (m1, m2) -> {
                m2.forEach((k, v) -> {
                    double[] acc = m1.computeIfAbsent(k, kk -> new double[2]);
                    acc[0] += v[0];
                    acc[1] += v[1];
                });
                return m1;
            };
        }

        @Override
        public Function<Map<String, double[]>, List<ItemSales>> finisher() {
            return map -> map.entrySet().stream()
                    .map(e -> new ItemSales(
                            e.getKey(),
                            (long) e.getValue()[0],
                            e.getValue()[1]
                    ))
                    .sorted(ItemSales.byQuantityAndRevenueDescending())
                    .toList();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

}
