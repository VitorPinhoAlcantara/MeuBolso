package br.com.meubolso.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExpenseByAccountProjection {
    UUID getAccountId();
    String getAccountName();
    BigDecimal getTotal();
}
