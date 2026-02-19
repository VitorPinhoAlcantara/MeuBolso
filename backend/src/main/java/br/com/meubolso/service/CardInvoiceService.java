package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.CardInvoice;
import br.com.meubolso.domain.PaymentMethod;
import br.com.meubolso.domain.enums.InvoiceStatus;
import br.com.meubolso.domain.enums.PaymentMethodType;
import br.com.meubolso.dto.CardInvoicePaymentRequest;
import br.com.meubolso.dto.CardInvoiceResponse;
import br.com.meubolso.dto.CardInvoiceUpdateRequest;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.CardInvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class CardInvoiceService {

    private final AccountRepository accountRepository;
    private final CardInvoiceRepository cardInvoiceRepository;

    public CardInvoiceService(AccountRepository accountRepository,
                              CardInvoiceRepository cardInvoiceRepository) {
        this.accountRepository = accountRepository;
        this.cardInvoiceRepository = cardInvoiceRepository;
    }

    public Page<CardInvoiceResponse> findAllByUser(UUID userId,
                                                   UUID accountId,
                                                   UUID paymentMethodId,
                                                   Integer year,
                                                   Integer month,
                                                   InvoiceStatus status,
                                                   Pageable pageable) {
        return cardInvoiceRepository.findByUserWithFilters(
                        userId, accountId, paymentMethodId, year, month, status, pageable)
                .map(this::toResponse);
    }

    public CardInvoiceResponse findById(UUID userId, UUID invoiceId) {
        CardInvoice invoice = cardInvoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura não encontrada"));
        return toResponse(invoice);
    }

    @Transactional
    public CardInvoiceResponse payInvoice(UUID userId, UUID invoiceId, CardInvoicePaymentRequest request) {
        CardInvoice invoice = cardInvoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura não encontrada"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fatura já está paga");
        }
        if (invoice.getTotalAmount() == null || invoice.getTotalAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fatura sem valor para pagamento");
        }

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta pagadora não encontrada"));
        if (!fromAccount.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        accountRepository.addToBalance(fromAccount.getId(), invoice.getTotalAmount().negate());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidFromAccountId(fromAccount.getId());
        invoice.setPaidAt(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());

        return toResponse(cardInvoiceRepository.save(invoice));
    }

    @Transactional
    public CardInvoiceResponse cancelPayment(UUID userId, UUID invoiceId) {
        CardInvoice invoice = cardInvoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura não encontrada"));

        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Apenas faturas pagas podem ser canceladas");
        }
        if (invoice.getPaidFromAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não há conta pagadora registrada para esta fatura");
        }

        Account paidFrom = accountRepository.findById(invoice.getPaidFromAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta pagadora não encontrada"));
        if (!paidFrom.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        accountRepository.addToBalance(paidFrom.getId(), invoice.getTotalAmount());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setPaidFromAccountId(null);
        invoice.setPaidAt(null);

        return toResponse(cardInvoiceRepository.save(invoice));
    }

    @Transactional
    public CardInvoiceResponse updateInvoice(UUID userId, UUID invoiceId, CardInvoiceUpdateRequest request) {
        CardInvoice invoice = cardInvoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura não encontrada"));

        BigDecimal current = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
        BigDecimal next = request.getTotalAmount() == null ? BigDecimal.ZERO : request.getTotalAmount();
        BigDecimal delta = next.subtract(current);

        if (invoice.getStatus() == InvoiceStatus.PAID && delta.signum() != 0) {
            if (invoice.getPaidFromAccountId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Fatura paga sem conta pagadora registrada");
            }
            Account paidFrom = accountRepository.findById(invoice.getPaidFromAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta pagadora não encontrada"));
            if (!paidFrom.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
            }
            accountRepository.addToBalance(paidFrom.getId(), delta.negate());
        }

        invoice.setTotalAmount(next);
        return toResponse(cardInvoiceRepository.save(invoice));
    }

    @Transactional
    public void deleteInvoice(UUID userId, UUID invoiceId) {
        CardInvoice invoice = cardInvoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura não encontrada"));

        if (invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaidFromAccountId() != null) {
            Account paidFrom = accountRepository.findById(invoice.getPaidFromAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta pagadora não encontrada"));
            if (!paidFrom.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
            }
            accountRepository.addToBalance(paidFrom.getId(), invoice.getTotalAmount());
        }

        cardInvoiceRepository.delete(invoice);
    }

    @Transactional
    public UUID resolveInvoiceIdForTransaction(UUID userId,
                                               Account account,
                                               PaymentMethod paymentMethod,
                                               LocalDate transactionDate) {
        if (paymentMethod.getType() != PaymentMethodType.CARD) {
            return null;
        }
        if (paymentMethod.getBillingClosingDay() == null || paymentMethod.getBillingDueDay() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Método CARD precisa de dia de fechamento e vencimento");
        }

        YearMonth period = resolveInvoicePeriod(transactionDate, paymentMethod.getBillingClosingDay());
        CardInvoice invoice = cardInvoiceRepository
                .findByUserIdAndPaymentMethodIdAndPeriodYearAndPeriodMonth(
                        userId, paymentMethod.getId(), period.getYear(), period.getMonthValue())
                .orElseGet(() -> {
                    CardInvoice created = new CardInvoice();
                    created.setUserId(userId);
                    created.setAccountId(account.getId());
                    created.setPaymentMethodId(paymentMethod.getId());
                    created.setPeriodYear(period.getYear());
                    created.setPeriodMonth(period.getMonthValue());
                    created.setClosingDate(resolveClosingDate(period, paymentMethod.getBillingClosingDay()));
                    created.setDueDate(resolveDueDate(
                            period,
                            paymentMethod.getBillingClosingDay(),
                            paymentMethod.getBillingDueDay()
                    ));
                    created.setStatus(InvoiceStatus.OPEN);
                    created.setTotalAmount(BigDecimal.ZERO);
                    return cardInvoiceRepository.save(created);
                });

        return invoice.getId();
    }

    @Transactional
    public void addTransactionDelta(UUID invoiceId, BigDecimal delta) {
        if (invoiceId == null || delta == null || delta.signum() == 0) {
            return;
        }
        CardInvoice invoice = cardInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura não encontrada"));

        if (invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaidFromAccountId() != null) {
            // Keep a paid invoice financially consistent when new expenses/incomes are posted.
            accountRepository.addToBalance(invoice.getPaidFromAccountId(), delta.negate());
        }

        BigDecimal current = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
        invoice.setTotalAmount(current.add(delta));
        cardInvoiceRepository.save(invoice);
    }

    private YearMonth resolveInvoicePeriod(LocalDate transactionDate, int closingDay) {
        YearMonth current = YearMonth.from(transactionDate);
        return transactionDate.getDayOfMonth() > closingDay ? current : current.minusMonths(1);
    }

    private LocalDate resolveClosingDate(YearMonth invoicePeriod, int closingDay) {
        return resolveDay(invoicePeriod.plusMonths(1), closingDay);
    }

    private LocalDate resolveDay(YearMonth yearMonth, int day) {
        int safeDay = Math.min(day, yearMonth.lengthOfMonth());
        return yearMonth.atDay(safeDay);
    }

    private LocalDate resolveDueDate(YearMonth invoicePeriod, int closingDay, int dueDay) {
        YearMonth closingMonth = invoicePeriod.plusMonths(1);
        YearMonth dueMonth = dueDay > closingDay ? closingMonth : closingMonth.plusMonths(1);
        return resolveDay(dueMonth, dueDay);
    }

    private CardInvoiceResponse toResponse(CardInvoice invoice) {
        CardInvoiceResponse response = new CardInvoiceResponse();
        response.setId(invoice.getId());
        response.setUserId(invoice.getUserId());
        response.setAccountId(invoice.getAccountId());
        response.setPaymentMethodId(invoice.getPaymentMethodId());
        response.setPeriodYear(invoice.getPeriodYear());
        response.setPeriodMonth(invoice.getPeriodMonth());
        response.setClosingDate(invoice.getClosingDate());
        response.setDueDate(invoice.getDueDate());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setStatus(invoice.getStatus());
        response.setPaidFromAccountId(invoice.getPaidFromAccountId());
        response.setPaidAt(invoice.getPaidAt());
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        return response;
    }
}
