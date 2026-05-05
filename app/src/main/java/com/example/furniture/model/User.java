package com.example.furniture.model;

/**
 * Model untuk user/pengguna aplikasi FurniSpace.
 * Data disimpan ke tabel SQLite 'users'.
 */
public class User {

    private int id;
    private String name;
    private String email;
    private String password;   // Simulasi — plain text (bukan untuk production)
    private String address;
    private String defaultPayment;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public User() {}

    public User(String name, String email, String password) {
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.address  = "";
        this.defaultPayment = "";
    }

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getAddress() { return address; }
    public String getDefaultPayment() { return defaultPayment; }

    // ─── Setters ─────────────────────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setAddress(String address) { this.address = address; }
    public void setDefaultPayment(String defaultPayment) { this.defaultPayment = defaultPayment; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
