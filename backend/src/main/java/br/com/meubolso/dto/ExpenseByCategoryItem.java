package br.com.meubolso.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ExpenseByCategoryItem {

    private UUID categoryId;
    private String categoryName;
    private BigDecimal total;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
