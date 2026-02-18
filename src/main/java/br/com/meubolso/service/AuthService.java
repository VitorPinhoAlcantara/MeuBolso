package br.com.meubolso.service;

import br.com.meubolso.domain.RefreshToken;
import br.com.meubolso.domain.User;
import br.com.meubolso.dto.AuthLoginRequest;
import br.com.meubolso.dto.AuthRefreshRequest;
import br.com.meubolso.dto.AuthRegisterRequest;
import br.com.meubolso.dto.AuthTokenResponse;
import br.com.meubolso.repository.RefreshTokenRepository;
import br.com.meubolso.repository.UserRepository;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthTokenResponse register(AuthRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return issueTokens(savedUser);
    }

    public AuthTokenResponse login(AuthLoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        return issueTokens(user);
    }

    public AuthTokenResponse refresh(AuthRefreshRequest request) {
        try {
            AuthenticatedUser authenticatedUser = jwtService.parseRefreshToken(request.getRefreshToken());

            RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido"));

            if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado ou revogado");
            }

            User user = userRepository.findById(authenticatedUser.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

            storedToken.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(storedToken);

            return issueTokens(user);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido");
        }
    }

    public void logout(AuthRefreshRequest request) {
        try {
            jwtService.parseRefreshToken(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            return;
        }

        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(storedToken -> {
                    if (storedToken.getRevokedAt() == null) {
                        storedToken.setRevokedAt(OffsetDateTime.now());
                        refreshTokenRepository.save(storedToken);
                    }
                });
    }

    private AuthTokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(jwtService.extractExpiration(refreshTokenValue));
        refreshTokenRepository.save(refreshToken);

        return new AuthTokenResponse(accessToken, refreshTokenValue, "Bearer");
    }
}
