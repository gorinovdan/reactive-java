package rj.lab2.statistics.aggregators.item_average_receipt;

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
import rj.lab2.statistics.ItemAverageReceipt;

/**
 * Custom collector that mirrors the behaviour from lab1 but supports optional artificial
 * delays when resolving item names. Only items that appear at least twice within the same
 * receipt are taken into account, matching the semantics of the original assignment.
 */
public class ItemAverageCollector implements Collector<Receipt, Map<String, double[]>, List<ItemAverageReceipt>> {

    private final long itemNameDelayMillis;

    public ItemAverageCollector() {
        this(0L);
    }

    public ItemAverageCollector(long itemNameDelayMillis) {
        this.itemNameDelayMillis = Math.max(0L, itemNameDelayMillis);
    }

    @Override
    public Supplier<Map<String, double[]>> supplier() {
        return HashMap::new;
    }

    @Override
    public BiConsumer<Map<String, double[]>, Receipt> accumulator() {
        return (acc, receipt) -> {
            Map<String, List<Item>> grouped = receipt.getItems().stream()
                    .collect(Collectors.groupingBy(this::resolveName));

            grouped.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue().size() >= 2)
                    .forEach(entry -> {
                        String itemName = entry.getKey();
                        double[] bucket = acc.computeIfAbsent(itemName, key -> new double[2]);
                        for (Item item : entry.getValue()) {
                            bucket[0] += item.getQuantity() * item.getUnitPrice(); // total amount
                            bucket[1] += 1; // occurrences
                        }
                    });
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
    public Function<Map<String, double[]>, List<ItemAverageReceipt>> finisher() {
        return map -> map.entrySet().stream()
                .map(entry -> {
                    double[] stats = entry.getValue();
                    long occurrences = (long) stats[1];
                    double average = occurrences > 0 ? stats[0] / occurrences : 0.0;
                    return new ItemAverageReceipt(entry.getKey(), occurrences, average);
                })
                .sorted(ItemAverageReceipt.byAverageReceiptDescending())
                .toList();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }

    private String resolveName(Item item) {
        return itemNameDelayMillis > 0 ? item.getName(itemNameDelayMillis) : item.getName();
    }
}
