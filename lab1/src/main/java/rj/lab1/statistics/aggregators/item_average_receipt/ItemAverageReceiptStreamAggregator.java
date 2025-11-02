package rj.lab1.statistics.aggregators.item_average_receipt;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ItemAverageReceipt;

import java.util.List;
import java.util.stream.Collectors;

public class ItemAverageReceiptStreamAggregator {

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts) {
        return receipts.stream()
                // разворачиваем все items, но только те, что встречаются >=2 раза в данном Receipt
                .flatMap(r -> r.getItems().stream()
                        .collect(Collectors.groupingBy(Item::getName))
                        .entrySet().stream()
                        .filter(e -> e.getValue().size() >= 2)
                        .flatMap(e -> e.getValue().stream()))
                // теперь группируем по имени товара
                .collect(Collectors.groupingBy(
                        Item::getName,
                        Collectors.mapping(
                                i -> i.getQuantity() * i.getUnitPrice(),
                                Collectors.toList()
                        )
                ))
                .entrySet().stream()
                .map(e -> {
                    List<Double> amounts = e.getValue();
                    double avg = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    return new ItemAverageReceipt(e.getKey(), amounts.size(), avg);
                })
                .sorted(ItemAverageReceipt.byAverageReceiptDescending())
                .toList();
    }

}
