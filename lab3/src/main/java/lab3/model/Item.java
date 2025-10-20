package lab3.model;

import lombok.Getter;
import lombok.Setter;
import lab3.util.LatencySimulator;

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
            LatencySimulator.emulateWork(delayMillis);
        }
        return name;
    }
}
