package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.Category;
import br.com.meubolso.domain.enums.CategoryType;
import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.TransactionCreateRequest;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.CategoryRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldRejectWhenCategoryTypeDoesNotMatchTransactionType() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Account account = new Account();
        account.setId(accountId);
        account.setUserId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUserId(userId);
        category.setType(CategoryType.EXPENSE);

        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setAccountId(accountId);
        request.setCategoryId(categoryId);
        request.setType(TransactionType.INCOME);
        request.setAmount(new BigDecimal("100.00"));
        request.setDate(LocalDate.now());

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.create(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(accountRepository).findById(accountId);
        verify(categoryRepository).findById(categoryId);
        verifyNoInteractions(transactionRepository);
    }
}
