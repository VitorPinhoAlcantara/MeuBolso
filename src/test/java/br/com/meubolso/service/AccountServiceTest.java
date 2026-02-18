package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldBlockDeleteWhenAccountHasTransactions() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        Account account = new Account();
        account.setId(accountId);
        account.setUserId(userId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.existsByUserIdAndAccountId(userId, accountId)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.delete(userId, accountId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(transactionRepository).existsByUserIdAndAccountId(userId, accountId);
    }
}
