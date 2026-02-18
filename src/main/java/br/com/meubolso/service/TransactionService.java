package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.Category;
import br.com.meubolso.domain.Transaction;
import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.TransactionCreateRequest;
import br.com.meubolso.dto.TransactionResponse;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.CategoryRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    public TransactionResponse create(UUID userId, TransactionCreateRequest request) {
        Account account = findOwnedAccount(userId, request.getAccountId());
        Category category = findOwnedCategory(userId, request.getCategoryId());
        TransactionType transactionType = request.getType();

        validateBusinessRules(account, category, transactionType, request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAccountId(account.getId());
        transaction.setCategoryId(category.getId());
        transaction.setType(transactionType);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(request.getDate());
        transaction.setDescription(request.getDescription());

        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    public Page<TransactionResponse> findAllByUser(UUID userId,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   TransactionType type,
                                                   UUID accountId,
                                                   UUID categoryId,
                                                   Pageable pageable) {
        return transactionRepository.findByUserWithFilters(userId, from, to, type, accountId, categoryId, pageable)
                .map(this::toResponse);
    }

    public TransactionResponse findById(UUID userId, UUID transactionId) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        return toResponse(transaction);
    }

    public TransactionResponse update(UUID userId, UUID transactionId, TransactionCreateRequest request) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        Account account = findOwnedAccount(userId, request.getAccountId());
        Category category = findOwnedCategory(userId, request.getCategoryId());
        TransactionType transactionType = request.getType();

        validateBusinessRules(account, category, transactionType, request.getAmount());

        transaction.setAccountId(account.getId());
        transaction.setCategoryId(category.getId());
        transaction.setType(transactionType);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(request.getDate());
        transaction.setDescription(request.getDescription());

        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    public void delete(UUID userId, UUID transactionId) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        transactionRepository.delete(transaction);
    }

    private void validateBusinessRules(Account account,
                                       Category category,
                                       TransactionType transactionType,
                                       BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor da transação deve ser maior que zero");
        }

        if (!category.getType().name().equals(transactionType.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo da categoria incompatível com o tipo da transação");
        }

        if (!account.getUserId().equals(category.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Conta e categoria devem pertencer ao mesmo usuário");
        }
    }

    private Transaction findOwnedTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada"));

        if (!transaction.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return transaction;
    }

    private Account findOwnedAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (!account.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return account;
    }

    private Category findOwnedCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));

        if (!category.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return category;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setUserId(transaction.getUserId());
        response.setAccountId(transaction.getAccountId());
        response.setCategoryId(transaction.getCategoryId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setDate(transaction.getTransactionDate());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}
