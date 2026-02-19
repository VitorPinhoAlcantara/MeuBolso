package br.com.meubolso.service;

import br.com.meubolso.config.StorageProperties;
import br.com.meubolso.domain.Transaction;
import br.com.meubolso.domain.TransactionAttachment;
import br.com.meubolso.dto.TransactionAttachmentResponse;
import br.com.meubolso.repository.TransactionAttachmentRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionAttachmentService {

    private final TransactionAttachmentRepository transactionAttachmentRepository;
    private final TransactionRepository transactionRepository;
    private final MinioStorageService minioStorageService;
    private final StorageProperties storageProperties;

    public TransactionAttachmentService(TransactionAttachmentRepository transactionAttachmentRepository,
                                        TransactionRepository transactionRepository,
                                        MinioStorageService minioStorageService,
                                        StorageProperties storageProperties) {
        this.transactionAttachmentRepository = transactionAttachmentRepository;
        this.transactionRepository = transactionRepository;
        this.minioStorageService = minioStorageService;
        this.storageProperties = storageProperties;
    }

    @Transactional
    public TransactionAttachmentResponse upload(UUID userId, UUID transactionId, MultipartFile file) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        validateFile(file);

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String objectKey = "transactions/%s/%s/%s-%s"
                .formatted(userId, transaction.getId(), UUID.randomUUID(), fileName);

        try {
            minioStorageService.upload(objectKey, file.getInputStream(), file.getSize(), contentType);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha no upload do anexo");
        }

        try {
            TransactionAttachment attachment = new TransactionAttachment();
            attachment.setTransactionId(transaction.getId());
            attachment.setFileName(fileName);
            attachment.setContentType(contentType);
            attachment.setSizeBytes(file.getSize());
            attachment.setStorageKey(objectKey);

            TransactionAttachment saved = transactionAttachmentRepository.save(attachment);
            return toResponse(saved);
        } catch (Exception ex) {
            minioStorageService.delete(objectKey);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao salvar metadados do anexo");
        }
    }

    public List<TransactionAttachmentResponse> list(UUID userId, UUID transactionId) {
        findOwnedTransaction(userId, transactionId);
        return transactionAttachmentRepository.findByTransactionIdAndUserId(transactionId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DownloadedAttachment download(UUID userId, UUID transactionId, UUID attachmentId) {
        TransactionAttachment attachment = transactionAttachmentRepository.findOwned(attachmentId, transactionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado"));

        byte[] content = minioStorageService.download(attachment.getStorageKey()).content();
        return new DownloadedAttachment(attachment.getFileName(), attachment.getContentType(), content);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId, UUID attachmentId) {
        TransactionAttachment attachment = transactionAttachmentRepository.findOwned(attachmentId, transactionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado"));

        minioStorageService.delete(attachment.getStorageKey());
        transactionAttachmentRepository.delete(attachment);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo obrigatório");
        }

        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Arquivo excede limite de " + storageProperties.getMaxFileSizeBytes() + " bytes");
        }
    }

    private String sanitizeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "arquivo";
        }
        return original.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Transaction findOwnedTransaction(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada"));
    }

    private TransactionAttachmentResponse toResponse(TransactionAttachment attachment) {
        TransactionAttachmentResponse response = new TransactionAttachmentResponse();
        response.setId(attachment.getId());
        response.setTransactionId(attachment.getTransactionId());
        response.setFileName(attachment.getFileName());
        response.setContentType(attachment.getContentType());
        response.setSizeBytes(attachment.getSizeBytes());
        response.setCreatedAt(attachment.getCreatedAt());
        return response;
    }

    public record DownloadedAttachment(String fileName, String contentType, byte[] content) {
    }
}
