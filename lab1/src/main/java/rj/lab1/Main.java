package rj.lab1;

import rj.lab1.generators.SimpleReceiptGenerator;
import rj.lab1.model.Receipt;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        SimpleReceiptGenerator generator = new SimpleReceiptGenerator()
                .withItemRange(2, 7)
                .withPriceRange(5.0, 200.0);

        List<Receipt> receipts = generator.generateMany(2000);

        System.out.println(receipts.size());
    }
}