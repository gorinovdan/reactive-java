package rj.lab2.statistics.aggregators.item_average_receipt;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import rj.lab2.model.Item;
import rj.lab2.model.Receipt;
import rj.lab2.statistics.ItemAverageReceipt;

public final class ItemAverageReceiptStreamAggregator {

    private ItemAverageReceiptStreamAggregator() {
    }

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts) {
        return aggregate(receipts, 0L);
    }

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts, long itemNameDelayMillis) {
        Function<Item, String> resolveName = item -> itemNameDelayMillis > 0
                ? item.getName(itemNameDelayMillis)
                : item.getName();

        return receipts.stream()
                .flatMap(receipt -> receipt.getItems().stream()
                        .collect(Collectors.groupingBy(resolveName))
                        .entrySet().stream()
                        .filter(entry -> entry.getKey() != null && entry.getValue().size() >= 2)
                        .flatMap(entry -> entry.getValue().stream()
                                .map(item -> new AbstractMap.SimpleEntry<>(
                                        entry.getKey(),
                                        item.getQuantity() * item.getUnitPrice()))))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.toList())))
                .entrySet().stream()
                .map(entry -> {
                    List<Double> amounts = entry.getValue();
                    double average = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    return new ItemAverageReceipt(entry.getKey(), amounts.size(), average);
                })
                .sorted(ItemAverageReceipt.byAverageReceiptDescending())
                .toList();
    }
}
