package br.com.meubolso.controller;

import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.TransactionAttachmentResponse;
import br.com.meubolso.dto.TransactionCreateRequest;
import br.com.meubolso.dto.TransactionResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.TransactionAttachmentService;
import br.com.meubolso.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionAttachmentService transactionAttachmentService;

    public TransactionController(TransactionService transactionService,
                                 TransactionAttachmentService transactionAttachmentService) {
        this.transactionService = transactionService;
        this.transactionAttachmentService = transactionAttachmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @Valid @RequestBody TransactionCreateRequest request) {
        return transactionService.create(currentUser.userId(), request);
    }

    @GetMapping
    public Page<TransactionResponse> findAll(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @RequestParam(required = false) LocalDate from,
                                             @RequestParam(required = false) LocalDate to,
                                             @RequestParam(required = false) TransactionType type,
                                             @RequestParam(required = false) UUID accountId,
                                             @RequestParam(required = false) UUID paymentMethodId,
                                             @RequestParam(required = false) UUID categoryId,
                                             @RequestParam(required = false, name = "q") String query,
                                             @PageableDefault(size = 20, sort = {"transactionDate", "createdAt"},
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.findAllByUser(
                currentUser.userId(), from, to, type, accountId, paymentMethodId, categoryId, query, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable UUID id) {
        return transactionService.findById(currentUser.userId(), id);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody TransactionCreateRequest request) {
        return transactionService.update(currentUser.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser currentUser,
                       @PathVariable UUID id) {
        transactionService.delete(currentUser.userId(), id);
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionAttachmentResponse uploadAttachment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                          @PathVariable UUID id,
                                                          @RequestPart("file") MultipartFile file) {
        return transactionAttachmentService.upload(currentUser.userId(), id, file);
    }

    @GetMapping("/{id}/attachments")
    public List<TransactionAttachmentResponse> listAttachments(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                               @PathVariable UUID id) {
        return transactionAttachmentService.list(currentUser.userId(), id);
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable UUID id,
                                                     @PathVariable UUID attachmentId) {
        TransactionAttachmentService.DownloadedAttachment downloaded = transactionAttachmentService
                .download(currentUser.userId(), id, attachmentId);

        String contentType = downloaded.contentType() == null ? "application/octet-stream" : downloaded.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloaded.fileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(downloaded.content());
    }

    @GetMapping("/{id}/attachments/{attachmentId}/preview")
    public ResponseEntity<byte[]> previewAttachment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                    @PathVariable UUID id,
                                                    @PathVariable UUID attachmentId) {
        TransactionAttachmentService.DownloadedAttachment downloaded = transactionAttachmentService
                .download(currentUser.userId(), id, attachmentId);

        String contentType = downloaded.contentType() == null ? "application/octet-stream" : downloaded.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(downloaded.content());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                 @PathVariable UUID id,
                                 @PathVariable UUID attachmentId) {
        transactionAttachmentService.delete(currentUser.userId(), id, attachmentId);
    }
}
