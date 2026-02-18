package br.com.meubolso.controller;

import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.TransactionCreateRequest;
import br.com.meubolso.dto.TransactionResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
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
                                             @RequestParam(required = false) UUID categoryId,
                                             @PageableDefault(size = 20, sort = {"transactionDate", "createdAt"},
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.findAllByUser(currentUser.userId(), from, to, type, accountId, categoryId, pageable);
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
}
