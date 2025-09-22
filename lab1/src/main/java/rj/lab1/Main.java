package rj.lab1;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatistics;
import rj.lab1.statistics.ReceiptStatisticsIterateCircleAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamCustomAggregator;

public class Main {

    private static final int[] DATASET_SIZES = { 5_000, 50_000, 250_000 };

    public static void main(String[] args) {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);

        for (int size : DATASET_SIZES) {
            List<Receipt> receipts = generator.generateMany(size);

            Measurement<ReceiptStatistics> iterative = measure(
                    () -> ReceiptStatisticsIterateCircleAggregator.aggregate(receipts));
            Measurement<ReceiptStatistics> stream = measure(
                    () -> ReceiptStatisticsStreamAggregator.aggregate(receipts));
            Measurement<ReceiptStatistics> customCollector = measure(
                    () -> ReceiptStatisticsStreamCustomAggregator.aggregate(receipts));

            System.out.printf("Dataset size: %,d receipts%n", size);
            System.out.printf(" - Iterative loop: %d ms%n", iterative.duration().toMillis());
            System.out.printf(" - Stream collectors: %d ms%n", stream.duration().toMillis());
            System.out.printf(" - Custom collector: %d ms%n%n", customCollector.duration().toMillis());

            if (size == DATASET_SIZES[0]) {
                System.out.println("Sample statistics (iterative variant):");
                System.out.println(iterative.result());
                System.out.println();
            }
        }
    }

    private static <T> Measurement<T> measure(Supplier<T> supplier) {
        Instant start = Instant.now();
        T result = supplier.get();
        Instant end = Instant.now();
        return new Measurement<>(result, Duration.between(start, end));
    }

    private record Measurement<T>(T result, Duration duration) {
    }
}
