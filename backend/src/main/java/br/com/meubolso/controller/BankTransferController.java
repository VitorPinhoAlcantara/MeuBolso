package br.com.meubolso.controller;

import br.com.meubolso.dto.BankTransferCreateRequest;
import br.com.meubolso.dto.BankTransferResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.BankTransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class BankTransferController {

    private final BankTransferService bankTransferService;

    public BankTransferController(BankTransferService bankTransferService) {
        this.bankTransferService = bankTransferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankTransferResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @Valid @RequestBody BankTransferCreateRequest request) {
        return bankTransferService.create(currentUser.userId(), request);
    }
}
