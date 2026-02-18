package br.com.meubolso.repository;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Page<Account> findByUserId(UUID userId, Pageable pageable);

    Page<Account> findByUserIdAndType(UUID userId, AccountType type, Pageable pageable);
}
