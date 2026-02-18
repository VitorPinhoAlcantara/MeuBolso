package br.com.meubolso.repository;

import br.com.meubolso.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

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
    List<Transaction> findByUserWithFilters(UUID userId,
                                            LocalDate from,
                                            LocalDate to,
                                            String type,
                                            UUID accountId,
                                            UUID categoryId);
}
