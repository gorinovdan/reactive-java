package rj.lab2.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;

import rj.lab2.model.Item;
import rj.lab2.model.Receipt;

/**
 * Utilities for calculating {@link ItemAverageReceipt} metrics independently
 * from the full receipt statistics aggregators. Supports artificial delays when
 * resolving item names.
 */
public final class ItemAverageReceiptMetrics {

    private ItemAverageReceiptMetrics() {
    }

    public static Collector<Receipt, ?, List<ItemAverageReceipt>> collector() {
        return collector(Item::getName);
    }

    public static Collector<Receipt, ?, List<ItemAverageReceipt>> collector(long itemNameDelayMillis) {
        return collector(item -> itemNameDelayMillis > 0 ? item.getName(itemNameDelayMillis) : item.getName());
    }

    public static Collector<Receipt, ?, List<ItemAverageReceipt>> collector(
            Function<Item, String> itemNameExtractor) {
        Function<Item, String> extractor = Objects.requireNonNull(itemNameExtractor, "itemNameExtractor");
        return Collector.of(
                () -> new ItemAverageAccumulator(extractor),
                ItemAverageAccumulator::add,
                ItemAverageAccumulator::combine,
                ItemAverageAccumulator::finish);
    }

    public static List<ItemAverageReceipt> calculate(Collection<Receipt> receipts) {
        return calculate(receipts, Item::getName);
    }

    public static List<ItemAverageReceipt> calculate(Collection<Receipt> receipts, long itemNameDelayMillis) {
        return calculate(receipts, item -> itemNameDelayMillis > 0 ? item.getName(itemNameDelayMillis) : item.getName());
    }

    public static List<ItemAverageReceipt> calculate(
            Collection<Receipt> receipts,
            Function<Item, String> itemNameExtractor) {
        if (receipts == null || receipts.isEmpty()) {
            return List.of();
        }
        return receipts.stream().collect(collector(itemNameExtractor));
    }

    public static ItemAverageAccumulator newAccumulator() {
        return newAccumulator(Item::getName);
    }

    public static ItemAverageAccumulator newAccumulator(long itemNameDelayMillis) {
        return newAccumulator(item -> itemNameDelayMillis > 0 ? item.getName(itemNameDelayMillis) : item.getName());
    }

    public static ItemAverageAccumulator newAccumulator(Function<Item, String> itemNameExtractor) {
        return new ItemAverageAccumulator(itemNameExtractor);
    }

    public static final class ItemAverageAccumulator {
        private final Function<Item, String> itemNameExtractor;
        private final Map<String, Stats> stats = new HashMap<>();

        private ItemAverageAccumulator(Function<Item, String> itemNameExtractor) {
            this.itemNameExtractor = Objects.requireNonNull(itemNameExtractor, "itemNameExtractor");
        }

        public void add(Receipt receipt) {
            if (receipt == null || receipt.getItems() == null || receipt.getItems().isEmpty()) {
                return;
            }
            double orderTotal = 0.0;
            Set<String> uniqueItemNames = new HashSet<>();
            for (Item item : receipt.getItems()) {
                if (item == null) {
                    continue;
                }
                orderTotal += item.getUnitPrice() * item.getQuantity();
                String itemName = itemNameExtractor.apply(item);
                if (itemName != null) {
                    uniqueItemNames.add(itemName);
                }
            }
            addResolved(orderTotal, uniqueItemNames);
        }

        public ItemAverageAccumulator combine(ItemAverageAccumulator other) {
            if (other == null || other.stats.isEmpty()) {
                return this;
            }
            other.stats.forEach((itemName, otherStats) -> stats.merge(
                    itemName,
                    otherStats.copy(),
                    (left, right) -> {
                        left.add(right);
                        return left;
                    }));
            return this;
        }

        public List<ItemAverageReceipt> finish() {
            if (stats.isEmpty()) {
                return List.of();
            }
            return stats.entrySet().stream()
                    .filter(entry -> entry.getValue().receiptCount > 2)
                    .map(entry -> {
                        Stats value = entry.getValue();
                        double average = value.receiptCount > 0
                                ? value.totalReceiptAmount / value.receiptCount
                                : 0.0;
                        return new ItemAverageReceipt(entry.getKey(), value.receiptCount, average);
                    })
                    .sorted(ItemAverageReceipt.byAverageReceiptDescending())
                    .toList();
        }

        public void addResolved(double orderTotal, Set<String> uniqueItemNames) {
            if (uniqueItemNames == null || uniqueItemNames.isEmpty()) {
                return;
            }
            for (String itemName : uniqueItemNames) {
                if (itemName == null) {
                    continue;
                }
                stats.compute(itemName, (name, existing) -> {
                    if (existing == null) {
                        return new Stats(1L, orderTotal);
                    }
                    existing.add(orderTotal);
                    return existing;
                });
            }
        }
    }

    private static final class Stats {
        private long receiptCount;
        private double totalReceiptAmount;

        private Stats(long receiptCount, double totalReceiptAmount) {
            this.receiptCount = receiptCount;
            this.totalReceiptAmount = totalReceiptAmount;
        }

        private void add(double orderTotal) {
            receiptCount++;
            totalReceiptAmount += orderTotal;
        }

        private void add(Stats other) {
            receiptCount += other.receiptCount;
            totalReceiptAmount += other.totalReceiptAmount;
        }

        private Stats copy() {
            return new Stats(receiptCount, totalReceiptAmount);
        }
    }
}
