package br.com.meubolso.controller;

import br.com.meubolso.dto.AuthLoginRequest;
import br.com.meubolso.dto.AuthRefreshRequest;
import br.com.meubolso.dto.AuthRegisterRequest;
import br.com.meubolso.dto.AuthTokenResponse;
import br.com.meubolso.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
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
}
