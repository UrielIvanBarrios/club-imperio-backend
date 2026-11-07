package com.clubimperio.gestionclub.seguridad.dtos;

import com.clubimperio.gestionclub.seguridad.entities.Rol;

// Todos los campos son opcionales: solo se actualiza lo que viene distinto de null.
// "password" permite que un ADMIN le resetee la contraseña a otro usuario.
public record UsuarioUpdateRequest(
        String email,
        String nombre,
        Rol rol,
        Boolean activo,
        String password
) {
}
