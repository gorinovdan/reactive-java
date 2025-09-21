package rj.lab1.statistics.Collectors;

import rj.lab1.model.Receipt;
import rj.lab1.statistics.ReceiptStatistics;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ReceiptStatisticsCollector implements Collector<Receipt, ReceiptStatisticsAccumulator, ReceiptStatistics> {

    public static ReceiptStatisticsCollector toStatistics() {
        return new ReceiptStatisticsCollector();
    }

    @Override
    public Supplier<ReceiptStatisticsAccumulator> supplier() {
        return ReceiptStatisticsAccumulator::new;
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
