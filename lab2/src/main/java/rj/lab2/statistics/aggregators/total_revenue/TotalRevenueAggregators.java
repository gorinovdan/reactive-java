package rj.lab2.statistics.aggregators.total_revenue;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import rj.lab2.model.Item;
import rj.lab2.model.Receipt;

public final class TotalRevenueAggregators {

    private TotalRevenueAggregators() {
    }

    public static double circleAggregate(List<Receipt> receipts) {
        double totalRevenue = 0.0;
        for (Receipt receipt : receipts) {
            for (Item item : receipt.getItems()) {
                totalRevenue += item.getQuantity() * item.getUnitPrice();
            }
        }
        return totalRevenue;
    }

    public static double streamAggregate(List<Receipt> receipts) {
        return receipts.stream()
                .flatMap(receipt -> receipt.getItems().stream())
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
    }

    public static double customCollectorAggregate(List<Receipt> receipts) {
        return receipts.stream().collect(new TotalRevenueCollector());
    }

    public static final class TotalRevenueCollector implements Collector<Receipt, double[], Double> {

        @Override
        public Supplier<double[]> supplier() {
            return () -> new double[1];
        }

        @Override
        public BiConsumer<double[], Receipt> accumulator() {
            return (acc, receipt) -> {
                for (Item item : receipt.getItems()) {
                    acc[0] += item.getQuantity() * item.getUnitPrice();
                }
            };
        }

        @Override
        public BinaryOperator<double[]> combiner() {
            return (left, right) -> {
                left[0] += right[0];
                return left;
            };
        }

        @Override
        public Function<double[], Double> finisher() {
            return totals -> totals[0];
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }
}
