package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.Category;
import br.com.meubolso.domain.PaymentMethod;
import br.com.meubolso.domain.Transaction;
import br.com.meubolso.domain.enums.PaymentMethodType;
import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.TransactionCreateRequest;
import br.com.meubolso.dto.TransactionResponse;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.CategoryRepository;
import br.com.meubolso.repository.PaymentMethodRepository;
import br.com.meubolso.repository.TransactionAttachmentRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionAttachmentRepository transactionAttachmentRepository;
    private final MinioStorageService minioStorageService;
    private final CardInvoiceService cardInvoiceService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CategoryRepository categoryRepository,
                              PaymentMethodRepository paymentMethodRepository,
                              TransactionAttachmentRepository transactionAttachmentRepository,
                              MinioStorageService minioStorageService,
                              CardInvoiceService cardInvoiceService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionAttachmentRepository = transactionAttachmentRepository;
        this.minioStorageService = minioStorageService;
        this.cardInvoiceService = cardInvoiceService;
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionCreateRequest request) {
        PaymentMethod paymentMethod = resolvePaymentMethod(userId, request);
        Account account = findOwnedAccount(userId, paymentMethod.getAccountId());
        Category category = findOwnedCategory(userId, request.getCategoryId());
        TransactionType transactionType = request.getType();

        validateBusinessRules(account, category, transactionType, request.getAmount());

        int installments = request.getInstallments() == null ? 1 : request.getInstallments();
        if (installments > 1 && paymentMethod.getType() != PaymentMethodType.CARD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parcelamento está disponível apenas para método do tipo CARD");
        }

        LocalDate firstInstallmentDate = request.getFirstInstallmentDate() == null
                ? request.getDate()
                : request.getFirstInstallmentDate();
        LocalDate purchaseDate = request.getPurchaseDate() == null ? request.getDate() : request.getPurchaseDate();

        UUID installmentGroupId = installments > 1 ? UUID.randomUUID() : null;
        List<BigDecimal> installmentValues = splitInstallments(request.getAmount(), installments);

        Transaction firstSaved = null;
        for (int i = 0; i < installments; i++) {
            LocalDate installmentDate = firstInstallmentDate.plusMonths(i);
            BigDecimal installmentAmount = installmentValues.get(i);
            UUID invoiceId = cardInvoiceService.resolveInvoiceIdForTransaction(
                    userId, account, paymentMethod, installmentDate);

            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setAccountId(account.getId());
            transaction.setPaymentMethodId(paymentMethod.getId());
            transaction.setCategoryId(category.getId());
            transaction.setType(transactionType);
            transaction.setAmount(installmentAmount);
            transaction.setTransactionDate(installmentDate);
            transaction.setPurchaseDate(purchaseDate);
            transaction.setInstallmentGroupId(installmentGroupId);
            transaction.setInstallmentNumber(i + 1);
            transaction.setInstallmentTotal(installments);
            transaction.setInvoiceId(invoiceId);
            transaction.setDescription(request.getDescription());

            Transaction saved = transactionRepository.save(transaction);
            if (firstSaved == null) {
                firstSaved = saved;
            }

            accountRepository.addToBalance(account.getId(),
                    accountImpact(paymentMethod.getType(), transactionType, installmentAmount));
            cardInvoiceService.addTransactionDelta(invoiceId, invoiceDelta(transactionType, installmentAmount));
        }

        return toResponse(firstSaved);
    }

    public Page<TransactionResponse> findAllByUser(UUID userId,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   TransactionType type,
                                                   UUID accountId,
                                                   UUID paymentMethodId,
                                                   UUID categoryId,
                                                   String query,
                                                   Pageable pageable) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.of(1970, 1, 1);
        LocalDate effectiveTo = to != null ? to : LocalDate.of(2999, 12, 31);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido: data inicial maior que final");
        }

        String sanitizedQuery = query == null ? "" : query.trim().toLowerCase();
        Page<Transaction> page = transactionRepository.findByUserWithFilters(
                userId, effectiveFrom, effectiveTo, type, accountId, paymentMethodId, categoryId, sanitizedQuery, pageable);

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
        if (transaction.getInstallmentTotal() != null && transaction.getInstallmentTotal() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Atualização de transação parcelada ainda não é suportada");
        }
        if (request.getInstallments() != null && request.getInstallments() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use criação de nova transação para parcelamento");
        }

        TransactionType oldType = transaction.getType();
        BigDecimal oldAmount = transaction.getAmount();
        UUID oldInvoiceId = transaction.getInvoiceId();
        PaymentMethod oldPaymentMethod = findOwnedPaymentMethod(userId, transaction.getPaymentMethodId());
        PaymentMethod paymentMethod = resolvePaymentMethod(userId, request);
        Account oldAccount = findOwnedAccount(userId, oldPaymentMethod.getAccountId());
        Account account = findOwnedAccount(userId, paymentMethod.getAccountId());
        Category category = findOwnedCategory(userId, request.getCategoryId());
        TransactionType transactionType = request.getType();

        validateBusinessRules(account, category, transactionType, request.getAmount());

        LocalDate newDate = request.getDate();
        LocalDate purchaseDate = request.getPurchaseDate() == null ? newDate : request.getPurchaseDate();
        UUID newInvoiceId = cardInvoiceService.resolveInvoiceIdForTransaction(
                userId, account, paymentMethod, newDate);

        transaction.setAccountId(account.getId());
        transaction.setPaymentMethodId(paymentMethod.getId());
        transaction.setCategoryId(category.getId());
        transaction.setType(transactionType);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(newDate);
        transaction.setPurchaseDate(purchaseDate);
        transaction.setInvoiceId(newInvoiceId);
        transaction.setDescription(request.getDescription());

        Transaction saved = transactionRepository.save(transaction);

        accountRepository.addToBalance(oldAccount.getId(),
                accountImpact(oldPaymentMethod.getType(), oldType, oldAmount).negate());
        accountRepository.addToBalance(account.getId(),
                accountImpact(paymentMethod.getType(), transactionType, request.getAmount()));
        cardInvoiceService.addTransactionDelta(oldInvoiceId, invoiceDelta(oldType, oldAmount).negate());
        cardInvoiceService.addTransactionDelta(newInvoiceId, invoiceDelta(transactionType, request.getAmount()));

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        if (transaction.getInstallmentGroupId() != null && transaction.getInstallmentTotal() != null
                && transaction.getInstallmentTotal() > 1) {
            List<Transaction> installments = transactionRepository
                    .findByUserIdAndInstallmentGroupIdOrderByInstallmentNumberAsc(userId, transaction.getInstallmentGroupId());
            installments.forEach(this::deleteSingleTransaction);
            return;
        }
        deleteSingleTransaction(transaction);
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

    private BigDecimal accountImpact(PaymentMethodType methodType, TransactionType type, BigDecimal amount) {
        if (methodType == PaymentMethodType.CARD) {
            return BigDecimal.ZERO;
        }
        return balanceDelta(type, amount);
    }

    private BigDecimal invoiceDelta(TransactionType type, BigDecimal amount) {
        return type == TransactionType.EXPENSE ? amount : amount.negate();
    }

    private List<BigDecimal> splitInstallments(BigDecimal totalAmount, int installments) {
        BigDecimal perInstallment = totalAmount
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal consumed = perInstallment.multiply(BigDecimal.valueOf(installments));
        BigDecimal remainder = totalAmount.subtract(consumed);

        return java.util.stream.IntStream.range(0, installments)
                .mapToObj(i -> i == installments - 1 ? perInstallment.add(remainder) : perInstallment)
                .toList();
    }

    private void deleteSingleTransaction(Transaction transaction) {
        Account account = findOwnedAccount(transaction.getUserId(), transaction.getAccountId());
        PaymentMethod paymentMethod = findOwnedPaymentMethod(transaction.getUserId(), transaction.getPaymentMethodId());
        transactionAttachmentRepository.findByTransactionIdAndUserId(transaction.getId(), transaction.getUserId())
                .forEach(attachment -> minioStorageService.delete(attachment.getStorageKey()));
        transactionAttachmentRepository.deleteByTransactionId(transaction.getId());
        accountRepository.addToBalance(account.getId(),
                accountImpact(paymentMethod.getType(), transaction.getType(), transaction.getAmount()).negate());
        cardInvoiceService.addTransactionDelta(transaction.getInvoiceId(),
                invoiceDelta(transaction.getType(), transaction.getAmount()).negate());
        transactionRepository.delete(transaction);
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

    private PaymentMethod resolvePaymentMethod(UUID userId, TransactionCreateRequest request) {
        if (request.getPaymentMethodId() != null) {
            return findOwnedPaymentMethod(userId, request.getPaymentMethodId());
        }
        if (request.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "paymentMethodId é obrigatório quando accountId não for informado");
        }

        Account account = findOwnedAccount(userId, request.getAccountId());
        return paymentMethodRepository.findFirstByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(
                        userId, account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Conta não possui método de pagamento"));
    }

    private PaymentMethod findOwnedPaymentMethod(UUID userId, UUID paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pagamento não encontrado"));

        if (!paymentMethod.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return paymentMethod;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setUserId(transaction.getUserId());
        response.setAccountId(transaction.getAccountId());
        response.setPaymentMethodId(transaction.getPaymentMethodId());
        response.setCategoryId(transaction.getCategoryId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setDate(transaction.getTransactionDate());
        response.setPurchaseDate(transaction.getPurchaseDate());
        response.setInstallmentGroupId(transaction.getInstallmentGroupId());
        response.setInstallmentNumber(transaction.getInstallmentNumber());
        response.setInstallmentTotal(transaction.getInstallmentTotal());
        response.setInvoiceId(transaction.getInvoiceId());
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
        response.setPaymentMethodId(transaction.getPaymentMethodId());
        response.setCategoryId(transaction.getCategoryId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setDate(transaction.getTransactionDate());
        response.setPurchaseDate(transaction.getPurchaseDate());
        response.setInstallmentGroupId(transaction.getInstallmentGroupId());
        response.setInstallmentNumber(transaction.getInstallmentNumber());
        response.setInstallmentTotal(transaction.getInstallmentTotal());
        response.setInvoiceId(transaction.getInvoiceId());
        response.setDescription(transaction.getDescription());
        response.setAttachmentsCount(attachmentsCount);
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}
