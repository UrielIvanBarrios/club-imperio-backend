package com.clubimperio.gestionclub.membresia.dtos;

public record PersonaUpdateRequest(
        String nombre,
        String apellido,
        String email,
        String telefonoPrincipal,
        Boolean activo
) {
}
