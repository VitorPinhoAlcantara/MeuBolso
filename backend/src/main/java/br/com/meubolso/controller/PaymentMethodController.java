package br.com.meubolso.controller;

import br.com.meubolso.dto.PaymentMethodCreateRequest;
import br.com.meubolso.dto.PaymentMethodResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.PaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping("/accounts/{accountId}/payment-methods")
    public List<PaymentMethodResponse> findByAccount(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable UUID accountId) {
        return paymentMethodService.findByAccount(currentUser.userId(), accountId);
    }

    @PostMapping("/accounts/{accountId}/payment-methods")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable UUID accountId,
                                        @Valid @RequestBody PaymentMethodCreateRequest request) {
        return paymentMethodService.create(currentUser.userId(), accountId, request);
    }

    @PutMapping("/payment-methods/{id}")
    public PaymentMethodResponse update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody PaymentMethodCreateRequest request) {
        return paymentMethodService.update(currentUser.userId(), id, request);
    }

    @DeleteMapping("/payment-methods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser currentUser,
                       @PathVariable UUID id) {
        paymentMethodService.delete(currentUser.userId(), id);
    }
}
