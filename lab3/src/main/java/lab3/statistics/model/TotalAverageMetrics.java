package lab3.statistics.model;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collector;

import lab3.model.Receipt;

/**
 * Utilities for calculating {@link TotalAverage} metrics independently from the
 * full receipt statistics aggregators.
 */
public final class TotalAverageMetrics {

    private TotalAverageMetrics() {
    }

    public static TotalAverage calculate(Collection<Receipt> receipts) {
        if (receipts == null || receipts.isEmpty()) {
            return TotalAverage.empty();
        }
        return receipts.stream().collect(collector());
    }

    public static Collector<Receipt, ?, TotalAverage> collector() {
        return Collector.of(
                TotalAverageAccumulator::new,
                TotalAverageAccumulator::add,
                TotalAverageAccumulator::combine,
                TotalAverageAccumulator::finish);
    }

    public static TotalAverageAccumulator newAccumulator() {
        return new TotalAverageAccumulator();
    }

    public static final class TotalAverageAccumulator {
        private long receiptCount = 0;
        private double totalRevenue = 0;

        public void add(Receipt receipt) {
            if (receipt == null || receipt.getItems() == null) {
                return;
            }
            double orderTotal = receipt.getItems().stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                    .sum();
            addResolved(orderTotal);
        }

        public void addResolved(double orderTotal) {
            receiptCount++;
            totalRevenue += orderTotal;
        }

        public TotalAverageAccumulator combine(TotalAverageAccumulator other) {
            if (other == null) {
                return this;
            }
            this.receiptCount += other.receiptCount;
            this.totalRevenue += other.totalRevenue;
            return this;
        }

        public TotalAverage finish() {
            if (receiptCount == 0) {
                return TotalAverage.empty();
            }
            double average = totalRevenue / receiptCount;
            return new TotalAverage(receiptCount, totalRevenue, average);
        }
    }
}

