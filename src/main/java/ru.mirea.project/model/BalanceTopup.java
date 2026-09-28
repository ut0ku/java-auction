package ru.mirea.project.model;

import ru.mirea.project.util.MoscowTime;
import ru.mirea.project.util.MoneyFormatter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BalanceTopup {
    private Integer id;
    private Integer userId;
    private String buyerName;
    private BigDecimal amount;
    private LocalDateTime topupTime;
    private BigDecimal balanceAfter;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return buyerName;
    }

    public void setUserName(String buyerName) {
        this.buyerName = buyerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTopupTime() {
        return topupTime;
    }

    public void setTopupTime(LocalDateTime topupTime) {
        this.topupTime = topupTime;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    @Override
    public String toString() {
        return String.format("[%d] покупатель #%d %s | +%s руб. | баланс после: %s руб. | %s",
            id, userId, buyerName, MoneyFormatter.format(amount), MoneyFormatter.format(balanceAfter),
            MoscowTime.format(topupTime));
    }
}
