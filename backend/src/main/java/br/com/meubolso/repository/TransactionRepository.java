package br.com.meubolso.repository;

import br.com.meubolso.domain.Transaction;
import br.com.meubolso.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  boolean existsByUserIdAndAccountId(UUID userId, UUID accountId);

  boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);

  @Query("""
          select t from Transaction t
          where t.userId = :userId
            and (:from is null or t.transactionDate >= :from)
            and (:to is null or t.transactionDate <= :to)
            and (:type is null or t.type = :type)
            and (:accountId is null or t.accountId = :accountId)
            and (:categoryId is null or t.categoryId = :categoryId)
          order by t.transactionDate desc, t.createdAt desc
          """)
  Page<Transaction> findByUserWithFilters(UUID userId,
                                          LocalDate from,
                                          LocalDate to,
                                          TransactionType type,
                                          UUID accountId,
                                          UUID categoryId,
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
              coalesce(sum(t.amount), 0) as total
          from Transaction t
          join Category c on c.id = t.categoryId
          where t.userId = :userId
            and t.type = :type
            and t.transactionDate >= :startDate
            and t.transactionDate <= :endDate
          group by t.categoryId, c.name
          order by total desc
          """)
  java.util.List<ExpenseByCategoryProjection> sumExpensesByCategory(UUID userId,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    TransactionType type);

}
