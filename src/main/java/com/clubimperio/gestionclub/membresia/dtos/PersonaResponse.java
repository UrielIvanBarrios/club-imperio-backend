package com.clubimperio.gestionclub.membresia.dtos;

import com.clubimperio.gestionclub.membresia.entities.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PersonaResponse(
        UUID personaId,
        String dni,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String telefonoPrincipal,
        String email,
        Boolean activo,
        LocalDateTime fechaCreacion,
        boolean esSocioActivo,
        List<MembresiaResponse> membresias
) {
    public static PersonaResponse from(Persona persona) {
        return new PersonaResponse(
                persona.getPersonaId(),
                persona.getDni(),
                persona.getNombre(),
                persona.getApellido(),
                persona.getFechaNacimiento(),
                persona.getTelefonoPrincipal(),
                persona.getEmail(),
                persona.getActivo(),
                persona.getFechaCreacion(),
                persona.isSocioActivo(),
                persona.getMembresias().stream().map(MembresiaResponse::from).toList()
        );
    }
}
