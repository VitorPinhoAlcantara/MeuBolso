package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.BankTransfer;
import br.com.meubolso.dto.BankTransferCreateRequest;
import br.com.meubolso.dto.BankTransferResponse;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.BankTransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class BankTransferService {

    private final AccountRepository accountRepository;
    private final BankTransferRepository bankTransferRepository;

    public BankTransferService(AccountRepository accountRepository,
                               BankTransferRepository bankTransferRepository) {
        this.accountRepository = accountRepository;
        this.bankTransferRepository = bankTransferRepository;
    }

    @Transactional
    public BankTransferResponse create(UUID userId, BankTransferCreateRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta de origem e destino devem ser diferentes");
        }

        Account from = findOwnedAccount(userId, request.getFromAccountId());
        Account to = findOwnedAccount(userId, request.getToAccountId());

        accountRepository.addToBalance(from.getId(), request.getAmount().negate());
        accountRepository.addToBalance(to.getId(), request.getAmount());

        BankTransfer transfer = new BankTransfer();
        transfer.setUserId(userId);
        transfer.setFromAccountId(from.getId());
        transfer.setToAccountId(to.getId());
        transfer.setAmount(request.getAmount());
        transfer.setTransferDate(request.getDate());
        transfer.setDescription(request.getDescription());

        return toResponse(bankTransferRepository.save(transfer));
    }

    private Account findOwnedAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        if (!account.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
        return account;
    }

    private BankTransferResponse toResponse(BankTransfer transfer) {
        BankTransferResponse response = new BankTransferResponse();
        response.setId(transfer.getId());
        response.setUserId(transfer.getUserId());
        response.setFromAccountId(transfer.getFromAccountId());
        response.setToAccountId(transfer.getToAccountId());
        response.setAmount(transfer.getAmount());
        response.setDate(transfer.getTransferDate());
        response.setDescription(transfer.getDescription());
        response.setCreatedAt(transfer.getCreatedAt());
        return response;
    }
}
