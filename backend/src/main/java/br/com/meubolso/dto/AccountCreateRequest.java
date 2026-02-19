package br.com.meubolso.dto;

import br.com.meubolso.domain.enums.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AccountCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private AccountType type;

    @Size(min = 3, max = 3)
    private String currency;

    @Digits(integer = 12, fraction = 2)
    private BigDecimal balance;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
