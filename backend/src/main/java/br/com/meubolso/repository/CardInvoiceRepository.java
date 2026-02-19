package br.com.meubolso.repository;

import br.com.meubolso.domain.CardInvoice;
import br.com.meubolso.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface CardInvoiceRepository extends JpaRepository<CardInvoice, UUID> {

    Optional<CardInvoice> findByIdAndUserId(UUID id, UUID userId);

    Optional<CardInvoice> findByUserIdAndPaymentMethodIdAndPeriodYearAndPeriodMonth(
            UUID userId, UUID paymentMethodId, Integer periodYear, Integer periodMonth);

    @Query("""
            select i from CardInvoice i
            where i.userId = :userId
              and (:accountId is null or i.accountId = :accountId)
              and (:paymentMethodId is null or i.paymentMethodId = :paymentMethodId)
              and (:year is null or i.periodYear = :year)
              and (:month is null or i.periodMonth = :month)
              and (:status is null or i.status = :status)
            """)
    Page<CardInvoice> findByUserWithFilters(UUID userId,
                                            UUID accountId,
                                            UUID paymentMethodId,
                                            Integer year,
                                            Integer month,
                                            InvoiceStatus status,
                                            Pageable pageable);

    @Modifying
    @Query("update CardInvoice i set i.totalAmount = coalesce(i.totalAmount, 0) + :delta where i.id = :invoiceId")
    int addToTotal(UUID invoiceId, BigDecimal delta);
}
