package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.dto.AccountCreateRequest;
import br.com.meubolso.dto.AccountResponse;
import br.com.meubolso.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse create(UUID userId, AccountCreateRequest request) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(request.getName());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency() == null ? "BRL" : request.getCurrency());

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    public List<AccountResponse> findAllByUser(UUID userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse findById(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }

        return toResponse(account);
    }

    public AccountResponse update(UUID userId, UUID accountId, AccountCreateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }

        account.setName(request.getName());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency() == null ? "BRL" : request.getCurrency());

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    public void delete(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
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
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        return response;
    }
}
