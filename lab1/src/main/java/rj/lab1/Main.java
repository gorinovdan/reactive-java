package rj.lab1;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatisticsIterateCircleAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamCustomAggregator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);

        List<Receipt> receipts = generator.generateMany(2500000);

        Instant start = Instant.now();
        ReceiptStatisticsIterateCircleAggregator.aggregate(receipts);
        Instant end = Instant.now();

        Duration duration = Duration.between(start, end);
        System.out.println("Duration: " + duration.toMillis() + " ms");

        Instant startStream = Instant.now();
        ReceiptStatisticsStreamAggregator.aggregate(receipts);
        Instant endStream = Instant.now();

        System.out.println("Duration: " + Duration.between(startStream, endStream).toMillis() + " ms");

        Instant startCustomStream = Instant.now();
        ReceiptStatisticsStreamCustomAggregator.aggregate(receipts);
        Instant endCustomStream = Instant.now();

        System.out.println("Duration: " + Duration.between(startCustomStream, endCustomStream).toMillis() + " ms");
    }
}