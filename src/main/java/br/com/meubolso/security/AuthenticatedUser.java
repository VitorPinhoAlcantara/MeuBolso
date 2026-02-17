package br.com.meubolso.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email) {
}
