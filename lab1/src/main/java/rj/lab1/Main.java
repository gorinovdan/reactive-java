package rj.lab1;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatistics;
import rj.lab1.statistics.ReceiptStatisticsIterateCircleAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamAggregator;
import rj.lab1.statistics.ReceiptStatisticsStreamCustomAggregator;

public class Main {

    private static final int[] DATASET_SIZES = { 5_000, 50_000, 250_000 };
    private static final double DOUBLE_TOLERANCE = 1e-5;

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

            ensureConsistent(iterative.result(), stream.result(), customCollector.result());

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

    private static void ensureConsistent(ReceiptStatistics reference, ReceiptStatistics... others) {
        for (ReceiptStatistics other : others) {
            checkDouble("totalRevenue", reference.getTotalRevenue(), other.getTotalRevenue());
            checkDouble("averageReceiptAmount", reference.getAverageReceiptAmount(), other.getAverageReceiptAmount());
            checkDouble("minReceiptAmount", reference.getMinReceiptAmount(), other.getMinReceiptAmount());
            checkDouble("maxReceiptAmount", reference.getMaxReceiptAmount(), other.getMaxReceiptAmount());

            checkLong("totalOrders", reference.getTotalOrders(), other.getTotalOrders());
            checkLong("totalItemsSold", reference.getTotalItemsSold(), other.getTotalItemsSold());
            checkLong("uniqueCustomers", reference.getUniqueCustomers(), other.getUniqueCustomers());
            checkLong("totalLoyaltyPoints", reference.getTotalLoyaltyPoints(), other.getTotalLoyaltyPoints());

            checkOrdersByStatus(reference.getOrdersByStatus(), other.getOrdersByStatus());
            checkRevenueByMonth(reference.getRevenueByMonth(), other.getRevenueByMonth());
        }
    }

    private static void checkOrdersByStatus(Map<?, Long> expected, Map<?, Long> actual) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException("Orders by status mismatch: expected " + expected + ", actual " + actual);
        }
    }

    private static void checkRevenueByMonth(Map<Integer, Double> expected, Map<Integer, Double> actual) {
        Set<Integer> months = new HashSet<>(expected.keySet());
        months.addAll(actual.keySet());
        for (Integer month : months) {
            double expectedValue = expected.getOrDefault(month, 0.0);
            double actualValue = actual.getOrDefault(month, 0.0);
            checkDouble("revenueByMonth[" + month + "]", expectedValue, actualValue);
        }
    }

    private static void checkDouble(String label, double expected, double actual) {
        if (Math.abs(expected - actual) > DOUBLE_TOLERANCE) {
            throw new IllegalStateException(label + " mismatch: expected=" + expected + ", actual=" + actual);
        }
    }

    private static void checkLong(String label, long expected, long actual) {
        if (expected != actual) {
            throw new IllegalStateException(label + " mismatch: expected=" + expected + ", actual=" + actual);
        }
    }

    private record Measurement<T>(T result, Duration duration) {
    }
}
