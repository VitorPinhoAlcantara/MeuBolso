package br.com.meubolso.repository;

import br.com.meubolso.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(UUID userId, UUID accountId);

    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndAccountIdAndId(UUID userId, UUID accountId, UUID id);

    boolean existsByUserIdAndAccountIdAndNameIgnoreCase(UUID userId, UUID accountId, String name);

    Optional<PaymentMethod> findFirstByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(UUID userId, UUID accountId);

    boolean existsByUserIdAndAccountId(UUID userId, UUID accountId);
}
