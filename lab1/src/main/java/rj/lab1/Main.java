package rj.lab1;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatisticsIterateCircleAggregator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);

        List<Receipt> receipts = generator.generateMany(250000);

        Instant start = Instant.now();
        var stat = ReceiptStatisticsIterateCircleAggregator.aggregate(receipts);
        Instant end = Instant.now();

        Duration duration = Duration.between(start, end);

        System.out.println(stat);
        System.out.println("Duration: " + duration.toMillis() + " ms");
    }
}