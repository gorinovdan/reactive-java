package lab3.benchmarks;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lab3.generators.SimpleReceiptGenerator;
import lab3.model.Receipt;
import lab3.statistics.Collectors.ReceiptStatisticsCollector;
import lab3.statistics.model.ReceiptStatistics;
import lab3.util.LatencySimulator;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class ReactiveReceiptStatisticsFlowableDemo {

    public static void main(String[] args) throws InterruptedException {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(1, 8)
                .withPriceRange(5, 1000);

        int totalReceipts = 10_000;          // количество чеков для примера
        int requestBatchSize = 200;          // сколько элементов Subscriber запрашивает за раз
        long itemDelayMs = 5L;               // задержка для имитации "работы" при обработке

        // создаём Flowable, который асинхронно генерирует чеки
        Flowable<Receipt> receiptFlowable = Flowable.<Receipt, AtomicInteger>generate(
                () -> new AtomicInteger(0),
                (counter, emitter) -> {
                    int index = counter.getAndIncrement();
                    if (index >= totalReceipts) {
                        emitter.onComplete();
                    } else {
                        emitter.onNext(generator.generateOne());
                    }
                }
                )
                .subscribeOn(Schedulers.io())   // генерация идёт на отдельном Scheduler
                .onBackpressureBuffer(10_000);  // буферизация, если потребитель отстаёт

        // создаём и подписываем кастомного Subscriber-а
        CountDownLatch completion = new CountDownLatch(1);
        ReceiptStatisticsSubscriber subscriber =
                new ReceiptStatisticsSubscriber(requestBatchSize, itemDelayMs, completion);

        receiptFlowable.subscribe(subscriber);

        // дожидаемся завершения обработки (для демонстрации)
        boolean finished = completion.await(30, TimeUnit.SECONDS);
        if (!finished) {
            System.err.println("Обработка чеков не завершилась за ожидаемое время.");
        }
    }

    /**
     * Собственный Subscriber, который накапливает ReceiptStatistics и управляет скоростью потребления.
     */
    public static class ReceiptStatisticsSubscriber implements Subscriber<Receipt> {
        private final int batchSize;
        private final long delayMillis;
        private final CountDownLatch completionLatch;
        private Subscription subscription;

        private final AtomicReference<ReceiptStatistics> statsRef = new AtomicReference<>(new ReceiptStatistics());
        private int processedInBatch = 0;

        public ReceiptStatisticsSubscriber(int batchSize, long delayMillis, CountDownLatch completionLatch) {
            this.batchSize = batchSize;
            this.delayMillis = delayMillis;
            this.completionLatch = completionLatch;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            System.out.println("Подписка установлена. Запрашиваю первый пакет из " + batchSize + " чеков...");
            s.request(batchSize); // первый запрос
        }

        @Override
        public void onNext(Receipt receipt) {
            try {
                // Обрабатываем чек — обновляем статистику через коллектор
                ReceiptStatistics newStats = processReceipt(receipt);
                statsRef.set(mergeStatistics(statsRef.get(), newStats));

                processedInBatch++;
                if (processedInBatch >= batchSize) {
                    processedInBatch = 0;
                    System.out.println("Запрашиваю следующий пакет из " + batchSize + " чеков...");
                    subscription.request(batchSize);
                }

                if (delayMillis > 0) {
                    LatencySimulator.emulateWork(delayMillis); // имитация задержки при обработке
                }
            } catch (Exception e) {
                e.printStackTrace();
                completionLatch.countDown();
            }
        }

        @Override
        public void onError(Throwable t) {
            System.err.println("Ошибка в потоке данных: " + t.getMessage());
            t.printStackTrace();
            completionLatch.countDown();
        }

        @Override
        public void onComplete() {
            System.out.println("\nПоток завершён ✅");
            ReceiptStatistics stats = statsRef.get();
            System.out.println("Всего заказов: " + stats.getTotalOrders());
            System.out.println("Общая выручка: " + stats.getTotalRevenue());
            System.out.println("Средний чек: " + stats.getAverageReceiptAmount());
            System.out.println("Мин. чек: " + stats.getMinReceiptAmount());
            System.out.println("Макс. чек: " + stats.getMaxReceiptAmount());
            System.out.println("Уникальных клиентов: " + stats.getUniqueCustomers());
            completionLatch.countDown();
        }

        /**
         * Обработка одного Receipt через уже существующий ReceiptStatisticsCollector.
         */
        private ReceiptStatistics processReceipt(Receipt receipt) {
            return Flowable.just(receipt)
                    .collect(() -> new ReceiptStatistics(),
                            (acc, r) -> acc.merge(
                                    Flowable.just(r).toList()
                                            .blockingGet()
                                            .stream()
                                            .collect(ReceiptStatisticsCollector.withItemNameDelay(delayMillis))
                            ))
                    .blockingGet();
        }

        /**
         * Простое объединение статистики (аналог mergeStatistics из предыдущего класса).
         */
        private ReceiptStatistics mergeStatistics(ReceiptStatistics a, ReceiptStatistics b) {
            if (a == null) return b;
            if (b == null) return a;
            ReceiptStatistics merged = new ReceiptStatistics();

            merged.setTotalRevenue(a.getTotalRevenue() + b.getTotalRevenue());
            merged.setTotalOrders(a.getTotalOrders() + b.getTotalOrders());
            merged.setTotalItemsSold(a.getTotalItemsSold() + b.getTotalItemsSold());
            merged.setTotalLoyaltyPoints(a.getTotalLoyaltyPoints() + b.getTotalLoyaltyPoints());
            merged.setUniqueCustomers(a.getUniqueCustomers() + b.getUniqueCustomers());

            merged.setMinReceiptAmount(Math.min(a.getMinReceiptAmount(), b.getMinReceiptAmount()));
            merged.setMaxReceiptAmount(Math.max(a.getMaxReceiptAmount(), b.getMaxReceiptAmount()));

            double totalOrders = merged.getTotalOrders();
            merged.setAverageReceiptAmount(totalOrders > 0
                    ? merged.getTotalRevenue() / totalOrders
                    : 0);

            return merged;
        }
    }
}
