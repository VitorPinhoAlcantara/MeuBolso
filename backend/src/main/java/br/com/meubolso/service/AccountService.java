package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.enums.AccountType;
import br.com.meubolso.dto.AccountCreateRequest;
import br.com.meubolso.dto.AccountResponse;
import br.com.meubolso.repository.AccountRepository;
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

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public AccountResponse create(UUID userId, AccountCreateRequest request) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(request.getName());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency() == null ? "BRL" : request.getCurrency());
        account.setBalance(request.getBalance() == null ? BigDecimal.ZERO : request.getBalance());

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    public Page<AccountResponse> findAllByUser(UUID userId, AccountType type, Pageable pageable) {
        Page<Account> accounts = type == null
                ? accountRepository.findByUserId(userId, pageable)
                : accountRepository.findByUserIdAndType(userId, type, pageable);

        return accounts.map(this::toResponse);
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
        account.setType(request.getType());
        account.setCurrency(request.getCurrency() == null ? "BRL" : request.getCurrency());
        account.setBalance(request.getBalance() == null ? BigDecimal.ZERO : request.getBalance());

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
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        return response;
    }
}
