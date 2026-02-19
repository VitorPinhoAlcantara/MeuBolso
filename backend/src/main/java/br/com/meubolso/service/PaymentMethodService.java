package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.PaymentMethod;
import br.com.meubolso.domain.enums.PaymentMethodType;
import br.com.meubolso.dto.PaymentMethodCreateRequest;
import br.com.meubolso.dto.PaymentMethodResponse;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.PaymentMethodRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentMethodService {

    private final AccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;

    public PaymentMethodService(AccountRepository accountRepository,
                                PaymentMethodRepository paymentMethodRepository,
                                TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<PaymentMethodResponse> findByAccount(UUID userId, UUID accountId) {
        Account account = findOwnedAccount(userId, accountId);
        return paymentMethodRepository.findByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(userId, account.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentMethodResponse create(UUID userId, UUID accountId, PaymentMethodCreateRequest request) {
        Account account = findOwnedAccount(userId, accountId);
        validate(request);

        if (paymentMethodRepository.existsByUserIdAndAccountIdAndNameIgnoreCase(userId, account.getId(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe método com esse nome para esta conta");
        }

        PaymentMethod method = new PaymentMethod();
        method.setUserId(userId);
        method.setAccountId(account.getId());
        method.setName(request.getName());
        method.setType(request.getType());
        method.setBillingClosingDay(request.getType() == PaymentMethodType.CARD ? request.getBillingClosingDay() : null);
        method.setBillingDueDay(request.getType() == PaymentMethodType.CARD ? request.getBillingDueDay() : null);
        method.setCreditLimit(request.getType() == PaymentMethodType.CARD ? request.getCreditLimit() : null);
        method.setDefault(Boolean.TRUE.equals(request.getIsDefault())
                || !paymentMethodRepository.existsByUserIdAndAccountId(userId, account.getId()));

        if (method.isDefault()) {
            unsetDefaultMethods(userId, account.getId());
        }

        return toResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public PaymentMethodResponse update(UUID userId, UUID methodId, PaymentMethodCreateRequest request) {
        PaymentMethod method = paymentMethodRepository.findByIdAndUserId(methodId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pagamento não encontrado"));
        validate(request);

        boolean alreadyExistsWithName = paymentMethodRepository
                .findByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(userId, method.getAccountId()).stream()
                .anyMatch(existing -> !existing.getId().equals(methodId)
                        && existing.getName().equalsIgnoreCase(request.getName()));
        if (alreadyExistsWithName) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe método com esse nome para esta conta");
        }

        method.setName(request.getName());
        method.setType(request.getType());
        method.setBillingClosingDay(request.getType() == PaymentMethodType.CARD ? request.getBillingClosingDay() : null);
        method.setBillingDueDay(request.getType() == PaymentMethodType.CARD ? request.getBillingDueDay() : null);
        method.setCreditLimit(request.getType() == PaymentMethodType.CARD ? request.getCreditLimit() : null);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !method.isDefault()) {
            unsetDefaultMethods(userId, method.getAccountId());
            method.setDefault(true);
        }

        return toResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public void delete(UUID userId, UUID methodId) {
        PaymentMethod method = paymentMethodRepository.findByIdAndUserId(methodId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Método de pagamento não encontrado"));

        if (transactionRepository.existsByUserIdAndPaymentMethodId(userId, methodId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir método de pagamento com transações vinculadas");
        }

        List<PaymentMethod> allFromAccount = paymentMethodRepository
                .findByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(userId, method.getAccountId());
        if (allFromAccount.size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A conta precisa manter ao menos um método de pagamento");
        }

        boolean wasDefault = method.isDefault();
        paymentMethodRepository.delete(method);

        if (wasDefault) {
            paymentMethodRepository.findFirstByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(userId, method.getAccountId())
                    .ifPresent(next -> {
                        next.setDefault(true);
                        paymentMethodRepository.save(next);
                    });
        }
    }

    private void unsetDefaultMethods(UUID userId, UUID accountId) {
        paymentMethodRepository.findByUserIdAndAccountIdOrderByIsDefaultDescCreatedAtAsc(userId, accountId)
                .forEach(existing -> {
                    if (existing.isDefault()) {
                        existing.setDefault(false);
                        paymentMethodRepository.save(existing);
                    }
                });
    }

    private Account findOwnedAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        if (!account.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
        return account;
    }

    private void validate(PaymentMethodCreateRequest request) {
        if (request.getType() == PaymentMethodType.CARD) {
            if (request.getBillingClosingDay() == null || request.getBillingDueDay() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Método CARD exige dia de fechamento e dia de vencimento");
            }
            if (request.getCreditLimit() != null && request.getCreditLimit().signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite de crédito não pode ser negativo");
            }
        }
    }

    private PaymentMethodResponse toResponse(PaymentMethod method) {
        PaymentMethodResponse response = new PaymentMethodResponse();
        response.setId(method.getId());
        response.setUserId(method.getUserId());
        response.setAccountId(method.getAccountId());
        response.setName(method.getName());
        response.setType(method.getType());
        response.setBillingClosingDay(method.getBillingClosingDay());
        response.setBillingDueDay(method.getBillingDueDay());
        response.setCreditLimit(method.getCreditLimit());
        response.setIsDefault(method.isDefault());
        response.setCreatedAt(method.getCreatedAt());
        response.setUpdatedAt(method.getUpdatedAt());
        return response;
    }
}
