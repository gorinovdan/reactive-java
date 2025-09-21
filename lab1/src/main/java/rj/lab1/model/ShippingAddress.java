package rj.lab1.model;

record ShippingAddress(
        String recipientName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country
) {
}
