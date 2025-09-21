package rj.lab1.statistics;

import rj.lab1.model.Receipt;
import rj.lab1.statistics.Collectors.ReceiptStatisticsCollector;

import java.util.*;

public class ReceiptStatisticsStreamCustomAggregator {

    public static ReceiptStatistics aggregate(List<Receipt> receipts) {
        return receipts.stream().collect(ReceiptStatisticsCollector.toStatistics());
    }
}
