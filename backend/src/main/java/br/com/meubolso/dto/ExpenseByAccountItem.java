package br.com.meubolso.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ExpenseByAccountItem {

    private UUID accountId;
    private String accountName;
    private BigDecimal total;

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
