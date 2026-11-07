package com.clubimperio.gestionclub.seguridad.dtos;

public record LoginRequest(
        String email,
        String password
) {
}
