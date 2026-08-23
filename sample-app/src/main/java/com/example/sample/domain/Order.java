package com.example.sample.domain;

public class Order {

    private final Long id;
    private final String item;
    private final int quantity;
    private final String customerToken;

    public Order(Long id, String item, int quantity, String customerToken) {
        this.id = id;
        this.item = item;
        this.quantity = quantity;
        this.customerToken = customerToken;
    }

    public Long getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCustomerToken() {
        return customerToken;
    }
}
