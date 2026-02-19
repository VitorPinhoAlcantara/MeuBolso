package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.Category;
import br.com.meubolso.domain.Transaction;
import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.TransactionCreateRequest;
import br.com.meubolso.dto.TransactionResponse;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.CategoryRepository;
import br.com.meubolso.repository.TransactionAttachmentRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionAttachmentRepository transactionAttachmentRepository;
    private final MinioStorageService minioStorageService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CategoryRepository categoryRepository,
                              TransactionAttachmentRepository transactionAttachmentRepository,
                              MinioStorageService minioStorageService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionAttachmentRepository = transactionAttachmentRepository;
        this.minioStorageService = minioStorageService;
    }

    @Transactional
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
        accountRepository.addToBalance(account.getId(), balanceDelta(transactionType, request.getAmount()));
        return toResponse(saved);
    }

    public Page<TransactionResponse> findAllByUser(UUID userId,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   TransactionType type,
                                                   UUID accountId,
                                                   UUID categoryId,
                                                   String query,
                                                   Pageable pageable) {
        String sanitizedQuery = query == null ? "" : query.trim().toLowerCase();
        Page<Transaction> page = transactionRepository.findByUserWithFilters(
                userId, from, to, type, accountId, categoryId, sanitizedQuery, pageable);

        Map<UUID, Long> attachmentsCountByTransactionId = page.getContent().isEmpty()
                ? Map.of()
                : transactionAttachmentRepository
                        .countByTransactionIds(page.getContent().stream().map(Transaction::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                count -> count.getTransactionId(),
                                count -> count.getTotal(),
                                (a, b) -> a));

        return page.map(transaction ->
                toResponse(transaction, attachmentsCountByTransactionId.getOrDefault(transaction.getId(), 0L)));
    }

    public TransactionResponse findById(UUID userId, UUID transactionId) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        return toResponse(transaction, transactionAttachmentRepository.countByTransactionId(transaction.getId()));
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, TransactionCreateRequest request) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        TransactionType oldType = transaction.getType();
        BigDecimal oldAmount = transaction.getAmount();
        Account oldAccount = findOwnedAccount(userId, transaction.getAccountId());
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

        // Undo old effect and apply new effect (also handles account changes)
        accountRepository.addToBalance(oldAccount.getId(), balanceDelta(oldType, oldAmount).negate());
        accountRepository.addToBalance(account.getId(), balanceDelta(transactionType, request.getAmount()));

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        Account account = findOwnedAccount(userId, transaction.getAccountId());

        transactionAttachmentRepository.findByTransactionIdAndUserId(transactionId, userId)
                .forEach(attachment -> minioStorageService.delete(attachment.getStorageKey()));
        transactionAttachmentRepository.deleteByTransactionId(transactionId);

        accountRepository.addToBalance(account.getId(), balanceDelta(transaction.getType(), transaction.getAmount()).negate());
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

    private BigDecimal balanceDelta(TransactionType type, BigDecimal amount) {
        return type == TransactionType.INCOME ? amount : amount.negate();
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
        response.setAttachmentsCount(transactionAttachmentRepository.countByTransactionId(transaction.getId()));
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }

    private TransactionResponse toResponse(Transaction transaction, long attachmentsCount) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setUserId(transaction.getUserId());
        response.setAccountId(transaction.getAccountId());
        response.setCategoryId(transaction.getCategoryId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setDate(transaction.getTransactionDate());
        response.setDescription(transaction.getDescription());
        response.setAttachmentsCount(attachmentsCount);
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}
