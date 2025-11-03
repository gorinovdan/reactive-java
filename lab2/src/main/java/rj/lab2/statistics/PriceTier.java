package rj.lab2.statistics;

/**
 * Price tiers that group items by their unit price to build categorical
 * metrics.
 */
public enum PriceTier {
    BUDGET(0, 20),
    STANDARD(20, 100),
    PREMIUM(100, 250),
    LUXURY(250, Double.POSITIVE_INFINITY);

    private final double minInclusive;
    private final double maxExclusive;

    PriceTier(double minInclusive, double maxExclusive) {
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    public static PriceTier fromUnitPrice(double unitPrice) {
        for (PriceTier tier : values()) {
            if (unitPrice >= tier.minInclusive && unitPrice < tier.maxExclusive) {
                return tier;
            }
        }
        return LUXURY;
    }
}
