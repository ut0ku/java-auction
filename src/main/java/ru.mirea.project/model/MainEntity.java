package ru.mirea.project.model;

import ru.mirea.project.util.MoscowTime;
import ru.mirea.project.util.MoneyFormatter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MainEntity {
    private Integer id;
    private Integer sellerId;
    private String sellerName;
    private Integer categoryId;
    private String categoryName;
    private String title;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime endsAt;
    private String criminalRecord;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public String getCriminalRecord() {
        return criminalRecord;
    }

    public void setCriminalRecord(String criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    @Override
    public String toString() {
        String base = String.format("[%d] %s | %s | %s | %s | старт: %s | текущая: %s | %s — %s",
                id, title, categoryName, sellerName, status,
                MoneyFormatter.format(startingPrice), MoneyFormatter.format(currentPrice),
                MoscowTime.format(createdAt), MoscowTime.format(endsAt));
        if (criminalRecord != null && !criminalRecord.isBlank()) {
            base += String.format(" | ⚠ Внимание: на данную недвижимость наложена судимость: %s", criminalRecord);
        }
        return base;
    }
}
