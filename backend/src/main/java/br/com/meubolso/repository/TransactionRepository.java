package br.com.meubolso.repository;

import br.com.meubolso.domain.Transaction;
import br.com.meubolso.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

  List<Transaction> findByUserIdAndInstallmentGroupIdOrderByInstallmentNumberAsc(UUID userId, UUID installmentGroupId);

  boolean existsByUserIdAndAccountId(UUID userId, UUID accountId);

  boolean existsByUserIdAndPaymentMethodId(UUID userId, UUID paymentMethodId);

  boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);

  @Query("""
          select t from Transaction t
          join Account a on a.id = t.accountId
          join PaymentMethod pm on pm.id = t.paymentMethodId
          join Category c on c.id = t.categoryId
          where t.userId = :userId
            and t.transactionDate >= :from
            and t.transactionDate <= :to
            and (:type is null or t.type = :type)
            and (:accountId is null or t.accountId = :accountId)
            and (:paymentMethodId is null or t.paymentMethodId = :paymentMethodId)
            and (:categoryId is null or t.categoryId = :categoryId)
            and (
              :query = ''
              or lower(coalesce(t.description, '')) like concat('%', :query, '%')
              or lower(a.name) like concat('%', :query, '%')
              or lower(pm.name) like concat('%', :query, '%')
              or lower(c.name) like concat('%', :query, '%')
            )
          order by t.transactionDate desc, t.createdAt desc
          """)
  Page<Transaction> findByUserWithFilters(UUID userId,
                                          LocalDate from,
                                          LocalDate to,
                                          TransactionType type,
                                          UUID accountId,
                                          UUID paymentMethodId,
                                          UUID categoryId,
                                          String query,
                                          Pageable pageable);
  
  @Query(value = """
      select coalesce(sum(t.amount), 0)
      from transactions t
      join payment_methods pm on pm.id = t.payment_method_id
      left join card_invoices ci on ci.id = t.invoice_id
      where t.user_id = :userId
        and t.type = :type
        and (
          (pm.type = 'CARD' and ci.period_year = :year and ci.period_month = :month)
          or
          (pm.type <> 'CARD' and t.date >= :startDate and t.date <= :endDate)
        )
      """, nativeQuery = true)
  BigDecimal sumAmountByTypeAndPeriod(UUID userId,
                                      String type,
                                      Integer year,
                                      Integer month,
                                      LocalDate startDate,
                                      LocalDate endDate);

  @Query(value = """
          select
              t.category_id as categoryId,
              c.name as categoryName,
              c.color as categoryColor,
              coalesce(sum(t.amount), 0) as total
          from transactions t
          join categories c on c.id = t.category_id
          join payment_methods pm on pm.id = t.payment_method_id
          left join card_invoices ci on ci.id = t.invoice_id
          where t.user_id = :userId
            and t.type = :type
            and (
              (pm.type = 'CARD' and ci.period_year = :year and ci.period_month = :month)
              or
              (pm.type <> 'CARD' and t.date >= :startDate and t.date <= :endDate)
            )
          group by t.category_id, c.name, c.color
          order by total desc
          """, nativeQuery = true)
  java.util.List<ExpenseByCategoryProjection> sumExpensesByCategory(UUID userId,
                                                                    Integer year,
                                                                    Integer month,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    String type);

  @Query(value = """
          select
              t.account_id as accountId,
              a.name as accountName,
              coalesce(sum(t.amount), 0) as total
          from transactions t
          join accounts a on a.id = t.account_id
          join payment_methods pm on pm.id = t.payment_method_id
          left join card_invoices ci on ci.id = t.invoice_id
          where t.user_id = :userId
            and t.type = :type
            and (
              (pm.type = 'CARD' and ci.period_year = :year and ci.period_month = :month)
              or
              (pm.type <> 'CARD' and t.date >= :startDate and t.date <= :endDate)
            )
          group by t.account_id, a.name
          order by coalesce(sum(t.amount), 0) desc
          """, nativeQuery = true)
  java.util.List<ExpenseByAccountProjection> sumExpensesByAccount(UUID userId,
                                                                  Integer year,
                                                                  Integer month,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  String type);

}
