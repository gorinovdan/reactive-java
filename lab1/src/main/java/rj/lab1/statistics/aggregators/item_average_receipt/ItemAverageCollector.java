package rj.lab1.statistics.aggregators.item_average_receipt;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ItemAverageReceipt;

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

public class ItemAverageCollector implements Collector<Receipt, Map<String, double[]>, List<ItemAverageReceipt>> {

    @Override
    public Supplier<Map<String, double[]>> supplier() {
        return HashMap::new;
    }

    @Override
    public BiConsumer<Map<String, double[]>, Receipt> accumulator() {
        return (acc, receipt) -> {
            Map<String, List<Item>> grouped = receipt.getItems().stream()
                    .collect(Collectors.groupingBy(Item::getName));

            grouped.entrySet().stream()
                    .filter(e -> e.getValue().size() >= 2)
                    .forEach(e -> e.getValue().forEach(i -> {
                        double[] arr = acc.computeIfAbsent(i.getName(), k -> new double[2]);
                        arr[0] += i.getQuantity() * i.getUnitPrice(); // сумма
                        arr[1] += 1; // количество
                    }));
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
    public Function<Map<String, double[]>, List<ItemAverageReceipt>> finisher() {
        return map -> map.entrySet().stream()
                .map(e -> new ItemAverageReceipt(
                        e.getKey(),
                        (long) e.getValue()[1],
                        e.getValue()[0] / e.getValue()[1]
                ))
                .sorted(ItemAverageReceipt.byAverageReceiptDescending())
                .toList();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }
}

