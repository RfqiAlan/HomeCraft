package com.example.furniture.model;

/**
 * Model untuk pesanan pengguna.
 * Data disimpan ke tabel SQLite 'orders'.
 */
public class Order {

    // ─── Konstanta Status Order ──────────────────────────────────────────────────
    public static final String STATUS_WAITING  = "Waiting for Payment";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancelled";

    private int orderId;
    private int userId;
    private double totalPrice;
    private String paymentMethod;
    private String orderStatus;
    private String shippingAddress;
    private String createdAt;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public Order() {}

    public Order(int userId, double totalPrice, String paymentMethod,
                  String orderStatus, String shippingAddress, String createdAt) {
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.orderStatus = orderStatus;
        this.shippingAddress = shippingAddress;
        this.createdAt = createdAt;
    }

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getOrderId() { return orderId; }
    public int getUserId() { return userId; }
    public double getTotalPrice() { return totalPrice; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getOrderStatus() { return orderStatus; }
    public String getShippingAddress() { return shippingAddress; }
    public String getCreatedAt() { return createdAt; }

    // ─── Setters ─────────────────────────────────────────────────────────────────

    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Order{orderId=" + orderId + ", total=" + totalPrice
                + ", payment='" + paymentMethod + "', status='" + orderStatus + "'}";
    }
}
