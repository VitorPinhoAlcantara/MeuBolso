package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.PaymentMethod;
import br.com.meubolso.domain.enums.AccountType;
import br.com.meubolso.domain.enums.PaymentMethodType;
import br.com.meubolso.dto.AccountCreateRequest;
import br.com.meubolso.dto.AccountResponse;
import br.com.meubolso.dto.PaymentMethodResponse;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.PaymentMethodRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private static final String DEFAULT_CURRENCY = "BRL";
    private static final AccountType DEFAULT_ACCOUNT_TYPE = AccountType.BANK;

    private final AccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          PaymentMethodRepository paymentMethodRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
    }

    public AccountResponse create(UUID userId, AccountCreateRequest request) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(request.getName());
        account.setType(DEFAULT_ACCOUNT_TYPE);
        account.setCurrency(DEFAULT_CURRENCY);
        account.setBalance(request.getBalance() == null ? BigDecimal.ZERO : request.getBalance());
        account.setBillingClosingDay(null);
        account.setBillingDueDay(null);
        account.setCreditLimit(null);

        Account saved = accountRepository.save(account);
        PaymentMethod defaultMethod = new PaymentMethod();
        defaultMethod.setUserId(userId);
        defaultMethod.setAccountId(saved.getId());
        defaultMethod.setName("PIX");
        defaultMethod.setType(PaymentMethodType.PIX);
        defaultMethod.setDefault(true);
        paymentMethodRepository.save(defaultMethod);

        return toResponse(saved);
    }

    public Page<AccountResponse> findAllByUser(UUID userId, Pageable pageable) {
        return accountRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public AccountResponse findById(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return toResponse(account);
    }

    public AccountResponse update(UUID userId, UUID accountId, AccountCreateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        account.setName(request.getName());
        account.setType(DEFAULT_ACCOUNT_TYPE);
        account.setCurrency(DEFAULT_CURRENCY);
        account.setBalance(request.getBalance() == null ? BigDecimal.ZERO : request.getBalance());
        account.setBillingClosingDay(null);
        account.setBillingDueDay(null);
        account.setCreditLimit(null);

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    public void delete(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        if (transactionRepository.existsByUserIdAndAccountId(userId, accountId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível excluir conta com transações vinculadas");
        }

        accountRepository.delete(account);
    }

    private AccountResponse toResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUserId(account.getUserId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setCurrency(account.getCurrency());
        response.setBalance(account.getBalance());
        response.setPaymentMethods(paymentMethodRepository.findByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(
                account.getUserId(), account.getId()).stream().map(this::toPaymentMethodResponse).toList());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        return response;
    }

    private PaymentMethodResponse toPaymentMethodResponse(br.com.meubolso.domain.PaymentMethod method) {
        PaymentMethodResponse response = new PaymentMethodResponse();
        response.setId(method.getId());
        response.setUserId(method.getUserId());
        response.setAccountId(method.getAccountId());
        response.setName(method.getName());
        response.setType(method.getType());
        response.setBillingClosingDay(method.getBillingClosingDay());
        response.setBillingDueDay(method.getBillingDueDay());
        response.setCreditLimit(method.getCreditLimit());
        response.setDefault(method.isDefault());
        response.setCreatedAt(method.getCreatedAt());
        response.setUpdatedAt(method.getUpdatedAt());
        return response;
    }
}
