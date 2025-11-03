package rj.lab2.statistics.aggregators.item_average_receipt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import rj.lab2.model.Item;
import rj.lab2.model.Receipt;
import rj.lab2.statistics.ItemAverageReceipt;

public final class ItemAverageReceiptCircleAggregator {

    private ItemAverageReceiptCircleAggregator() {
    }

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts) {
        return aggregate(receipts, 0L);
    }

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts, long itemNameDelayMillis) {
        Map<String, List<Double>> grouped = new HashMap<>();

        for (Receipt receipt : receipts) {
            Map<String, List<Item>> itemsByName = receipt.getItems().stream()
                    .collect(Collectors.groupingBy(item -> resolveName(item, itemNameDelayMillis)));

            for (Map.Entry<String, List<Item>> entry : itemsByName.entrySet()) {
                String itemName = entry.getKey();
                if (itemName == null || entry.getValue().size() < 2) {
                    continue;
                }
                List<Double> amounts = grouped.computeIfAbsent(itemName, key -> new ArrayList<>());
                for (Item item : entry.getValue()) {
                    amounts.add(item.getQuantity() * item.getUnitPrice());
                }
            }
        }

        List<ItemAverageReceipt> stats = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : grouped.entrySet()) {
            List<Double> amounts = entry.getValue();
            double average = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            stats.add(new ItemAverageReceipt(entry.getKey(), amounts.size(), average));
        }
        stats.sort(ItemAverageReceipt.byAverageReceiptDescending());
        return stats;
    }

    private static String resolveName(Item item, long delayMillis) {
        return delayMillis > 0 ? item.getName(delayMillis) : item.getName();
    }
}
