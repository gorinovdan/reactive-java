package rj.lab1;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;
import rj.lab1.statistics.aggregators.complex.ReceiptStatisticsIterateCircleAggregator;
import rj.lab1.statistics.aggregators.complex.ReceiptStatisticsStreamAggregator;
import rj.lab1.statistics.aggregators.complex.ReceiptStatisticsStreamCustomAggregator;
import rj.lab1.statistics.aggregators.item_average_receipt.ItemAverageReceiptCircleAggregator;
import rj.lab1.statistics.aggregators.item_average_receipt.ItemAverageReceiptStreamAggregator;
import rj.lab1.statistics.aggregators.item_average_receipt.ItemAverageStreamCustomAggregator;
import rj.lab1.statistics.aggregators.top_items_by_quantity.TopItemsByQuantityAggregator;
import rj.lab1.statistics.aggregators.total_revenue.TotalRevenueAggregators;

import java.io.FileWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public class AggregationBenchmarkWithWarmUp {

    private static final int WARMUP_RUNS = 2;
    private static final int MEASURE_RUNS = 3;

    private static final Map<String, List<Long>> results = new LinkedHashMap<>();

    private static void benchmark(String label, Function<List<Receipt>, ?> aggregator, List<Receipt> receipts) {
        // --- Warm-up phase ---
        for (int i = 0; i < WARMUP_RUNS; i++) {
            aggregator.apply(receipts);
        }

        // --- Measurement phase ---
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < MEASURE_RUNS; i++) {
            Instant start = Instant.now();
            aggregator.apply(receipts);
            Instant end = Instant.now();
            times.add(Duration.between(start, end).toMillis());
        }

        // --- Compute average and std deviation ---
        long avg = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = times.stream().mapToDouble(t -> Math.pow(t - avg, 2)).sum() / times.size();
        double stddev = Math.sqrt(variance);

        System.out.printf("%-40s -> %4d ms (±%.1f)%n", label, avg, stddev);
        results.computeIfAbsent(label, k -> new ArrayList<>()).add(avg);
    }

    @SneakyThrows
    public static void main(String[] args) {
        int[] sizes = {5_000, 25_000, 250_000};
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator();

        for (int size : sizes) {
            System.out.println("\n===== Benchmark for " + size + " receipts =====");
            List<Receipt> receipts = generator.generateMany(size);

            // --- Собираем все тесты в список ---
            List<Map.Entry<String, Function<List<Receipt>, ?>>> tests = List.of(
                    Map.entry("TotalRevenue.circleAggregate", TotalRevenueAggregators::circleAggregate),
                    Map.entry("TotalRevenue.streamAggregate", TotalRevenueAggregators::streamAggregate),
                    Map.entry("TotalRevenue.customCollectorAggregate", TotalRevenueAggregators::customCollectorAggregate),

                    Map.entry("TopItemsByQuantity.circle", TopItemsByQuantityAggregator::topItemsByQuantityCircleAggregator),
                    Map.entry("TopItemsByQuantity.stream", TopItemsByQuantityAggregator::topItemsByQuantityStreamAggregator),
                    Map.entry("TopItemsByQuantity.collector", TopItemsByQuantityAggregator::topItemsByQuantity),

                    Map.entry("ItemAverageReceipt.circle", ItemAverageReceiptCircleAggregator::aggregate),
                    Map.entry("ItemAverageReceipt.stream", ItemAverageReceiptStreamAggregator::aggregate),
                    Map.entry("ItemAverageReceipt.collector", ItemAverageStreamCustomAggregator::aggregate),

                    Map.entry("ReceiptStatistics.circle", ReceiptStatisticsIterateCircleAggregator::aggregate),
                    Map.entry("ReceiptStatistics.stream", ReceiptStatisticsStreamAggregator::aggregate),
                    Map.entry("ReceiptStatistics.collector", ReceiptStatisticsStreamCustomAggregator::aggregate)
            );

            // --- Перемешиваем порядок выполнения ---
            List<Map.Entry<String, Function<List<Receipt>, ?>>> randomized = new ArrayList<>(tests);
            Collections.shuffle(randomized, new Random(42)); // фиксированный seed для воспроизводимости

            // --- Запускаем тесты в случайном порядке ---
            for (Map.Entry<String, Function<List<Receipt>, ?>> test : randomized) {
                benchmark(test.getKey(), test.getValue(), receipts);
                try {
                    Thread.sleep(50); // небольшая пауза для стабилизации GC
                } catch (InterruptedException ignored) {}
            }
        }

        // --- Сохраняем результаты в JSON ---
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter("benchmark_results.json")) {
            gson.toJson(results, writer);
        }
        System.out.println("Results saved to benchmark_results.json");
    }
}
