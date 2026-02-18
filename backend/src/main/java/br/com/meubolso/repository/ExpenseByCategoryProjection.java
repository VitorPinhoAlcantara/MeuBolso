package br.com.meubolso.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExpenseByCategoryProjection {
    UUID getCategoryId();
    String getCategoryName();
    String getCategoryColor();
    BigDecimal getTotal();
}
