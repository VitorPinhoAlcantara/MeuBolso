package br.com.meubolso.service;

import br.com.meubolso.domain.RefreshToken;
import br.com.meubolso.domain.User;
import br.com.meubolso.dto.AuthRefreshRequest;
import br.com.meubolso.dto.AuthTokenResponse;
import br.com.meubolso.repository.RefreshTokenRepository;
import br.com.meubolso.repository.UserRepository;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRotateRefreshTokenOnRefresh() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail("user@test.com");

        RefreshToken stored = new RefreshToken();
        stored.setToken("old-refresh");
        stored.setUserId(userId);
        stored.setExpiresAt(OffsetDateTime.now().plusDays(5));

        AuthRefreshRequest request = new AuthRefreshRequest();
        request.setRefreshToken("old-refresh");

        when(jwtService.parseRefreshToken("old-refresh")).thenReturn(new AuthenticatedUser(userId, user.getEmail()));
        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");
        when(jwtService.extractExpiration("new-refresh")).thenReturn(OffsetDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokenResponse response = authService.refresh(request);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        verify(refreshTokenRepository).findByToken("old-refresh");
        verify(refreshTokenRepository).save(eq(stored));
    }
}
