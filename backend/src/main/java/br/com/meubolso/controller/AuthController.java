package br.com.meubolso.controller;

import br.com.meubolso.dto.AuthLoginRequest;
import br.com.meubolso.dto.AuthEmailUpdateRequest;
import br.com.meubolso.dto.AuthDeleteAccountRequest;
import br.com.meubolso.dto.AuthMeResponse;
import br.com.meubolso.dto.AuthPasswordUpdateRequest;
import br.com.meubolso.dto.AuthProfileUpdateRequest;
import br.com.meubolso.dto.AuthRefreshRequest;
import br.com.meubolso.dto.AuthRegisterRequest;
import br.com.meubolso.dto.AuthTokenResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(security = {})
    @ResponseStatus(HttpStatus.CREATED)
    public AuthTokenResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(security = {})
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(security = {})
    public AuthTokenResponse refresh(@Valid @RequestBody AuthRefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(security = {})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthRefreshRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return authService.me(currentUser);
    }

    @PutMapping("/me/profile")
    public AuthMeResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @Valid @RequestBody AuthProfileUpdateRequest request) {
        return authService.updateProfile(currentUser.userId(), request);
    }

    @PutMapping("/me/email")
    public AuthMeResponse updateEmail(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @Valid @RequestBody AuthEmailUpdateRequest request) {
        return authService.updateEmail(currentUser.userId(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
                               @Valid @RequestBody AuthPasswordUpdateRequest request) {
        authService.updatePassword(currentUser.userId(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal AuthenticatedUser currentUser,
                              @Valid @RequestBody AuthDeleteAccountRequest request) {
        authService.deleteAccount(currentUser.userId(), request);
    }
}
