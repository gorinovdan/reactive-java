package rj.lab1.model;

public record ShippingAddress(
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country
) {
}
