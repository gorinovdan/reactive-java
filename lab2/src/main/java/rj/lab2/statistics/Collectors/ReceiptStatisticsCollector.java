package rj.lab1.statistics.Collectors;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatistics;

public class ReceiptStatisticsCollector implements Collector<Receipt, ReceiptStatisticsAccumulator, ReceiptStatistics> {

    private final long itemNameDelayMillis;

    public static ReceiptStatisticsCollector toStatistics() {
        return new ReceiptStatisticsCollector(0L);
    }

    public static ReceiptStatisticsCollector withItemNameDelay(long delayMillis) {
        return new ReceiptStatisticsCollector(delayMillis);
    }

    private ReceiptStatisticsCollector(long itemNameDelayMillis) {
        this.itemNameDelayMillis = Math.max(0L, itemNameDelayMillis);
    }

    @Override
    public Supplier<ReceiptStatisticsAccumulator> supplier() {
        return () -> new ReceiptStatisticsAccumulator(itemNameDelayMillis);
    }

    @Override
    public BiConsumer<ReceiptStatisticsAccumulator, Receipt> accumulator() {
        return ReceiptStatisticsAccumulator::add;
    }

    @Override
    public BinaryOperator<ReceiptStatisticsAccumulator> combiner() {
        return ReceiptStatisticsAccumulator::combine;
    }

    @Override
    public Function<ReceiptStatisticsAccumulator, ReceiptStatistics> finisher() {
        return ReceiptStatisticsAccumulator::toStatistics;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(); // нужен finisher, поэтому IDENTITY_FINISH не ставим
    }
}
