package br.com.meubolso.repository;

import br.com.meubolso.domain.BankTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BankTransferRepository extends JpaRepository<BankTransfer, UUID> {
}
