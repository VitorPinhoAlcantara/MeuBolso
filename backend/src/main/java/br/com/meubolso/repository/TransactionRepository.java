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
  
  @Query("""
      select coalesce(sum(t.amount), 0)
      from Transaction t
      where t.userId = :userId
        and t.type = :type
        and t.transactionDate >= :startDate
        and t.transactionDate <= :endDate
      """)
  BigDecimal sumAmountByTypeAndPeriod(UUID userId, TransactionType type, LocalDate startDate, LocalDate endDate);

  @Query("""
          select
              t.categoryId as categoryId,
              c.name as categoryName,
              c.color as categoryColor,
              coalesce(sum(t.amount), 0) as total
          from Transaction t
          join Category c on c.id = t.categoryId
          where t.userId = :userId
            and t.type = :type
            and t.transactionDate >= :startDate
            and t.transactionDate <= :endDate
          group by t.categoryId, c.name, c.color
          order by total desc
          """)
  java.util.List<ExpenseByCategoryProjection> sumExpensesByCategory(UUID userId,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    TransactionType type);

  @Query("""
          select
              t.accountId as accountId,
              a.name as accountName,
              coalesce(sum(t.amount), 0) as total
          from Transaction t
          join Account a on a.id = t.accountId
          where t.userId = :userId
            and t.type = :type
            and t.transactionDate >= :startDate
            and t.transactionDate <= :endDate
          group by t.accountId, a.name
          order by coalesce(sum(t.amount), 0) desc
          """)
  java.util.List<ExpenseByAccountProjection> sumExpensesByAccount(UUID userId,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  TransactionType type);

}
