package com.clubimperio.gestionclub.seguridad.dtos;

import com.clubimperio.gestionclub.seguridad.entities.Rol;

public record UsuarioCreateRequest(
        String email,
        String nombre,
        String password,
        Rol rol
) {
}
