package com.clubimperio.gestionclub.seguridad.dtos;

import com.clubimperio.gestionclub.seguridad.entities.Rol;
import com.clubimperio.gestionclub.seguridad.entities.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

// No incluye passwordHash: el hash nunca sale del back.
public record UsuarioResponse(
        UUID usuarioId,
        String email,
        String nombre,
        Rol rol,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaUltimaModificacion
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getUsuarioId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaCreacion(),
                usuario.getFechaUltimaModificacion()
        );
    }
}
