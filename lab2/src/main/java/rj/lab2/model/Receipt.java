package rj.lab2.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Receipt {
    private int loyaltyPointsEarned;
    private String id;
    private LocalDateTime date;
    private ReceiptStatus status;
    private ShippingAddress shippingAddress;
    private List<Item> items;
    private Customer customer;
}

