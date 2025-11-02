package rj.lab1.statistics.aggregators.item_average_receipt;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ItemAverageReceipt;

import java.util.*;
import java.util.stream.Collectors;

public class ItemAverageReceiptCircleAggregator {

    public static List<ItemAverageReceipt> aggregate(List<Receipt> receipts) {
        Map<String, List<Double>> grouped = new HashMap<>();

        for (Receipt r : receipts) {
            // фильтруем товары, которые в этом чеке встречаются >=2 раза
            Map<String, List<Item>> itemsByName = r.getItems().stream()
                    .collect(Collectors.groupingBy(Item::getName));

            for (Map.Entry<String, List<Item>> e : itemsByName.entrySet()) {
                if (e.getValue().size() >= 2) {
                    for (Item i : e.getValue()) {
                        grouped.computeIfAbsent(i.getName(), k -> new ArrayList<>())
                                .add(i.getQuantity() * i.getUnitPrice());
                    }
                }
            }
        }

        List<ItemAverageReceipt> stats = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : grouped.entrySet()) {
            List<Double> amounts = e.getValue();
            double avg = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            stats.add(new ItemAverageReceipt(e.getKey(), amounts.size(), avg));
        }

        stats.sort(ItemAverageReceipt.byAverageReceiptDescending());
        return stats;
    }

}
