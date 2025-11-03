package rj.lab2.generators;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import rj.lab2.model.Customer;
import rj.lab2.model.Item;
import rj.lab2.model.Receipt;
import rj.lab2.model.ReceiptStatus;
import rj.lab2.model.ShippingAddress;

public class SimpleReceiptGenerator {

    private int minItems = 1;
    private int maxItems = 5;
    private double minPrice = 1.0;
    private double maxPrice = 500.0;

    public SimpleReceiptGenerator withItemRange(int min, int max) {
        this.minItems = min;
        this.maxItems = max;
        return this;
    }

    public SimpleReceiptGenerator withPriceRange(double min, double max) {
        this.minPrice = min;
        this.maxPrice = max;
        return this;
    }

    public Receipt generateOne() {
        Receipt receipt = new Receipt();
        receipt.setId(RandomStringUtils.randomAlphanumeric(10));
        receipt.setDate(randomDateTime());
        receipt.setStatus(randomEnum(ReceiptStatus.class));
        receipt.setCustomer(randomCustomer());
        receipt.setShippingAddress(randomAddress());
        receipt.setItems(randomItems());

        // loyalty points считаем, например, как сумма цен/10
        int points = (int) receipt.getItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum() / 10;
        receipt.setLoyaltyPointsEarned(points);

        return receipt;
    }

    public List<Receipt> generateMany(int count) {
        List<Receipt> receipts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            receipts.add(generateOne());
        }
        return receipts;
    }

    private Customer randomCustomer() {
        Customer c = new Customer();
        c.setFirstName(RandomStringUtils.randomAlphabetic(5, 10));
        c.setLastName(RandomStringUtils.randomAlphabetic(5, 12));
        return c;
    }

    private ShippingAddress randomAddress() {
        return new ShippingAddress(
                RandomStringUtils.randomAlphabetic(10, 20) + " St.",
                RandomStringUtils.randomAlphabetic(5, 15) + " Apt.",
                RandomStringUtils.randomAlphabetic(5, 10),
                RandomStringUtils.randomAlphabetic(2, 5),
                RandomStringUtils.randomNumeric(5),
                "Utopia");
    }

    private List<Item> randomItems() {
        int count = RandomUtils.nextInt(minItems, maxItems + 1);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Item item = new Item();
            item.setName("Item-" + RandomStringUtils.randomAlphanumeric(4));
            item.setQuantity(RandomUtils.nextInt(1, 5));
            item.setUnitPrice(RandomUtils.nextDouble(minPrice, maxPrice));
            items.add(item);
        }
        return items;
    }

    private LocalDateTime randomDateTime() {
        long minDay = LocalDateTime.now().minusDays(365).toEpochSecond(java.time.ZoneOffset.UTC);
        long maxDay = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC);
        long randomEpoch = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        return LocalDateTime.ofEpochSecond(randomEpoch, 0, java.time.ZoneOffset.UTC);
    }

    private <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        T[] values = clazz.getEnumConstants();
        return values[RandomUtils.nextInt(0, values.length)];
    }
}
