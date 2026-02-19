package br.com.meubolso.dto;

import br.com.meubolso.domain.enums.PaymentMethodType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentMethodCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private PaymentMethodType type;

    @Min(1)
    @Max(31)
    private Integer billingClosingDay;

    @Min(1)
    @Max(31)
    private Integer billingDueDay;

    @Digits(integer = 12, fraction = 2)
    private BigDecimal creditLimit;

    private Boolean isDefault;

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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
