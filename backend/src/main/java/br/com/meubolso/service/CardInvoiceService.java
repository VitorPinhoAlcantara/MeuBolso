package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.CardInvoice;
import br.com.meubolso.domain.PaymentMethod;
import br.com.meubolso.domain.enums.InvoiceStatus;
import br.com.meubolso.domain.enums.PaymentMethodType;
import br.com.meubolso.dto.CardInvoiceResponse;
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

    private final CardInvoiceRepository cardInvoiceRepository;

    public CardInvoiceService(CardInvoiceRepository cardInvoiceRepository) {
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
                    created.setClosingDate(resolveDay(period, paymentMethod.getBillingClosingDay()));
                    created.setDueDate(resolveDay(period.plusMonths(1), paymentMethod.getBillingDueDay()));
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
        cardInvoiceRepository.addToTotal(invoiceId, delta);
    }

    private YearMonth resolveInvoicePeriod(LocalDate transactionDate, int closingDay) {
        YearMonth current = YearMonth.from(transactionDate);
        return transactionDate.getDayOfMonth() > closingDay ? current.plusMonths(1) : current;
    }

    private LocalDate resolveDay(YearMonth yearMonth, int day) {
        int safeDay = Math.min(day, yearMonth.lengthOfMonth());
        return yearMonth.atDay(safeDay);
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
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        return response;
    }
}
