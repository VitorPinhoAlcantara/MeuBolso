package br.com.meubolso.dto;

import br.com.meubolso.domain.enums.PaymentMethodType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentMethodResponse {

    private UUID id;
    private UUID userId;
    private UUID accountId;
    private String name;
    private PaymentMethodType type;
    private Integer billingClosingDay;
    private Integer billingDueDay;
    private BigDecimal creditLimit;
    private boolean isDefault;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PaymentMethodType getType() {
        return type;
    }

    public void setType(PaymentMethodType type) {
        this.type = type;
    }

    public Integer getBillingClosingDay() {
        return billingClosingDay;
    }

    public void setBillingClosingDay(Integer billingClosingDay) {
        this.billingClosingDay = billingClosingDay;
    }

    public Integer getBillingDueDay() {
        return billingDueDay;
    }

    public void setBillingDueDay(Integer billingDueDay) {
        this.billingDueDay = billingDueDay;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
