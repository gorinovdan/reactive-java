package rj.lab2.statistics.spliterator;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import rj.lab2.model.Receipt;

/**
 * Spliterator tuned for chunking {@link Receipt} lists into moderately sized batches.
 * It helps reducing overhead of very small tasks when using parallel streams.
 */
public final class ReceiptSpliterator implements Spliterator<Receipt> {

    private final List<Receipt> source;
    private final int fence;
    private int index;
    private final int minimumBatchSize;

    public ReceiptSpliterator(List<Receipt> source, int minimumBatchSize) {
        this(source, 0, source.size(), Math.max(1, minimumBatchSize));
    }

    private ReceiptSpliterator(List<Receipt> source, int origin, int fence, int minimumBatchSize) {
        this.source = source;
        this.index = origin;
        this.fence = fence;
        this.minimumBatchSize = Math.max(1, minimumBatchSize);
    }

    @Override
    public boolean tryAdvance(Consumer<? super Receipt> action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        if (index < fence) {
            action.accept(source.get(index++));
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(Consumer<? super Receipt> action) {
        Spliterator.super.forEachRemaining(action);
    }

    @Override
    public Spliterator<Receipt> trySplit() {
        int remaining = fence - index;
        if (remaining <= minimumBatchSize) {
            return null;
        }
        int splitSize = Math.max(remaining / 2, minimumBatchSize);
        int splitFence = index + splitSize;
        Spliterator<Receipt> split = new ReceiptSpliterator(source, index, splitFence, minimumBatchSize);
        index = splitFence;
        return split;
    }

    @Override
    public long estimateSize() {
        return fence - index;
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL;
    }
}
