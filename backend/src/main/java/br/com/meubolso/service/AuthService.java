package br.com.meubolso.service;

import br.com.meubolso.domain.Account;
import br.com.meubolso.domain.Category;
import br.com.meubolso.domain.RefreshToken;
import br.com.meubolso.domain.User;
import br.com.meubolso.domain.enums.AccountType;
import br.com.meubolso.domain.enums.CategoryType;
import br.com.meubolso.repository.AccountRepository;
import br.com.meubolso.dto.AuthLoginRequest;
import br.com.meubolso.dto.AuthMeResponse;
import br.com.meubolso.dto.AuthRefreshRequest;
import br.com.meubolso.dto.AuthRegisterRequest;
import br.com.meubolso.dto.AuthTokenResponse;
import br.com.meubolso.repository.CategoryRepository;
import br.com.meubolso.repository.RefreshTokenRepository;
import br.com.meubolso.repository.UserRepository;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       CategoryRepository categoryRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        createDefaultAccounts(savedUser);
        createDefaultCategories(savedUser);
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

    public AuthMeResponse me(AuthenticatedUser currentUser) {
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        AuthMeResponse response = new AuthMeResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        return response;
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

    private void createDefaultCategories(User user) {
        List<CategorySeed> seeds = List.of(
                new CategorySeed("Moradia", CategoryType.EXPENSE, "#2563EB"),
                new CategorySeed("Comida", CategoryType.EXPENSE, "#F97316"),
                new CategorySeed("Saúde", CategoryType.EXPENSE, "#EF4444"),
                new CategorySeed("Lazer", CategoryType.EXPENSE, "#A855F7"),
                new CategorySeed("Transporte", CategoryType.EXPENSE, "#0EA5E9"),
                new CategorySeed("Educação", CategoryType.EXPENSE, "#14B8A6"),
                new CategorySeed("Assinaturas", CategoryType.EXPENSE, "#8B5CF6"),
                new CategorySeed("Outros gastos", CategoryType.EXPENSE, "#F59E0B"),
                new CategorySeed("Salário", CategoryType.INCOME, "#16A34A"),
                new CategorySeed("Freelance", CategoryType.INCOME, "#22C55E"),
                new CategorySeed("Investimentos", CategoryType.INCOME, "#10B981"),
                new CategorySeed("Outras receitas", CategoryType.INCOME, "#06B6D4")
        );

        List<Category> categories = seeds.stream().map(seed -> {
            Category category = new Category();
            category.setUserId(user.getId());
            category.setName(seed.name());
            category.setType(seed.type());
            category.setColor(seed.color());
            return category;
        }).toList();

        categoryRepository.saveAll(categories);
    }

    private void createDefaultAccounts(User user) {
        Account wallet = new Account();
        wallet.setUserId(user.getId());
        wallet.setName("Carteira");
        wallet.setType(AccountType.CASH);
        wallet.setCurrency("BRL");
        wallet.setBalance(BigDecimal.ZERO);

        accountRepository.save(wallet);
    }

    private record CategorySeed(String name, CategoryType type, String color) {}
}
