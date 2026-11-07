package com.clubimperio.gestionclub.seguridad.dtos;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tipo,       // Siempre "Bearer": el front lo manda como "Authorization: Bearer <token>"
        Instant expiraEn,
        UsuarioResponse usuario
) {
}
