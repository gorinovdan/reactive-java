package rj.lab1.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Item {
    private String name;
    private int quantity;
    private double unitPrice;

    /**
     * Returns the item name after waiting for the requested artificial delay.
     * This helps imitate latency when benchmarking aggregation pipelines.
     *
     * @param delayMillis delay in milliseconds; non-positive values are ignored
     * @return item name
     */
    public String getName(long delayMillis) {
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return name;
    }
}
