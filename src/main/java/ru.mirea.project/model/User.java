package ru.mirea.project.model;

import ru.mirea.project.util.MoneyFormatter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User {
    private Integer id;
    private String fullName;
    private String email;
    private String phone;
    private BigDecimal balance;
    private LocalDateTime registeredAt;

    public User() {
    }

    public User(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s | баланс: %s руб. | %s",
                id, fullName, email, phone == null ? "-" : phone,
                MoneyFormatter.format(balance == null ? BigDecimal.ZERO : balance),
                registeredAt == null ? "-" : registeredAt);
    }
}
