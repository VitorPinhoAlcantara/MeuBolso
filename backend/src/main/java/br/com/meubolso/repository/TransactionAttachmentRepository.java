package br.com.meubolso.repository;

import br.com.meubolso.domain.TransactionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionAttachmentRepository extends JpaRepository<TransactionAttachment, UUID> {

    @Query("""
            select a
            from TransactionAttachment a
            join Transaction t on t.id = a.transactionId
            where a.transactionId = :transactionId
              and t.userId = :userId
            order by a.createdAt desc
            """)
    List<TransactionAttachment> findByTransactionIdAndUserId(UUID transactionId, UUID userId);

    @Query("""
            select a
            from TransactionAttachment a
            join Transaction t on t.id = a.transactionId
            where a.id = :attachmentId
              and a.transactionId = :transactionId
              and t.userId = :userId
            """)
    Optional<TransactionAttachment> findOwned(UUID attachmentId, UUID transactionId, UUID userId);

    long countByTransactionId(UUID transactionId);

    @Query("""
            select
              a.transactionId as transactionId,
              count(a.id) as total
            from TransactionAttachment a
            where a.transactionId in :transactionIds
            group by a.transactionId
            """)
    List<TransactionAttachmentCountProjection> countByTransactionIds(List<UUID> transactionIds);

    void deleteByTransactionId(UUID transactionId);
}
