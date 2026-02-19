package br.com.meubolso.controller;

import br.com.meubolso.domain.enums.InvoiceStatus;
import br.com.meubolso.dto.CardInvoicePaymentRequest;
import br.com.meubolso.dto.CardInvoiceResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.CardInvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class CardInvoiceController {

    private final CardInvoiceService cardInvoiceService;

    public CardInvoiceController(CardInvoiceService cardInvoiceService) {
        this.cardInvoiceService = cardInvoiceService;
    }

    @GetMapping
    public Page<CardInvoiceResponse> findAll(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @RequestParam(required = false) UUID accountId,
                                             @RequestParam(required = false) UUID paymentMethodId,
                                             @RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) Integer month,
                                             @RequestParam(required = false) InvoiceStatus status,
                                             @PageableDefault(size = 20, sort = {"periodYear", "periodMonth"},
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        return cardInvoiceService.findAllByUser(
                currentUser.userId(), accountId, paymentMethodId, year, month, status, pageable);
    }

    @GetMapping("/{id}")
    public CardInvoiceResponse findById(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable UUID id) {
        return cardInvoiceService.findById(currentUser.userId(), id);
    }

    @PostMapping("/{id}/pay")
    @ResponseStatus(HttpStatus.OK)
    public CardInvoiceResponse pay(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody CardInvoicePaymentRequest request) {
        return cardInvoiceService.payInvoice(currentUser.userId(), id, request);
    }

    @PostMapping("/{id}/cancel-payment")
    @ResponseStatus(HttpStatus.OK)
    public CardInvoiceResponse cancelPayment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @PathVariable UUID id) {
        return cardInvoiceService.cancelPayment(currentUser.userId(), id);
    }
}
