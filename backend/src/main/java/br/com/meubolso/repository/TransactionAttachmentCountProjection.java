package br.com.meubolso.repository;

import java.util.UUID;

public interface TransactionAttachmentCountProjection {
    UUID getTransactionId();
    Long getTotal();
}
