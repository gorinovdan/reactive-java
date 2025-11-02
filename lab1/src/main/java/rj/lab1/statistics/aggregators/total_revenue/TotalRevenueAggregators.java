package rj.lab1.statistics.aggregators.total_revenue;

import rj.lab1.model.Item;
import rj.lab1.model.Receipt;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class TotalRevenueAggregators {

    public static double circleAggregate(List<Receipt> receipts) {
        double totalRevenue = 0.0;
        for (Receipt r : receipts) {
            for (Item i : r.getItems()) {
                totalRevenue += i.getQuantity() * i.getUnitPrice();
            }
        }
        return totalRevenue;
    }

    public static double streamAggregate(List<Receipt> receipts) {
        return receipts.stream()
                .flatMap(r -> r.getItems().stream())
                .mapToDouble(i -> i.getQuantity() * i.getUnitPrice())
                .sum();
    }

    public static double customCollectorAggregate(List<Receipt> receipts) {
        return receipts.stream().collect(new TotalRevenueCollector());
    }


    public static class TotalRevenueCollector implements Collector<Receipt, double[], Double> {

        @Override
        public Supplier<double[]> supplier() {
            return () -> new double[1]; // [0] = totalRevenue
        }

        @Override
        public BiConsumer<double[], Receipt> accumulator() {
            return (acc, receipt) -> {
                for (Item i : receipt.getItems()) {
                    acc[0] += i.getQuantity() * i.getUnitPrice();
                }
            };
        }

        @Override
        public BinaryOperator<double[]> combiner() {
            return (a, b) -> {
                a[0] += b[0];
                return a;
            };
        }

        @Override
        public Function<double[], Double> finisher() {
            return a -> a[0];
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

}
