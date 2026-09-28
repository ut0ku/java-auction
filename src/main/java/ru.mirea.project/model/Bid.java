package ru.mirea.project.model;

import ru.mirea.project.util.MoscowTime;
import ru.mirea.project.util.MoneyFormatter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid {
    private Integer id;
    private Integer lotId;
    private String lotTitle;
    private Integer userId;
    private String buyerName;
    private BigDecimal amount;
    private LocalDateTime bidTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLotId() {
        return lotId;
    }

    public void setLotId(Integer lotId) {
        this.lotId = lotId;
    }

    public String getLotTitle() {
        return lotTitle;
    }

    public void setLotTitle(String lotTitle) {
        this.lotTitle = lotTitle;
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

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    @Override
    public String toString() {
        return String.format("[%d] лот #%d «%s» | покупатель #%d %s | %s руб. | %s",
            id, lotId, lotTitle, userId, buyerName, MoneyFormatter.format(amount), MoscowTime.format(bidTime));
    }
}
