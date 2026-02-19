package br.com.meubolso.controller;

import br.com.meubolso.dto.AccountCreateRequest;
import br.com.meubolso.dto.AccountResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                  @Valid @RequestBody AccountCreateRequest request) {
        return accountService.create(currentUser.userId(), request);
    }

    @GetMapping
    public Page<AccountResponse> findAll(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return accountService.findAllByUser(currentUser.userId(), pageable);
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable UUID id) {
        return accountService.findById(currentUser.userId(), id);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody AccountCreateRequest request) {
        return accountService.update(currentUser.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        accountService.delete(currentUser.userId(), id);
    }
}
