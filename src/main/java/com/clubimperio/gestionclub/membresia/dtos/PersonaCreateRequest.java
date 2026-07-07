package com.clubimperio.gestionclub.membresia.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PersonaCreateRequest(
        String dni,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String telefonoPrincipal,
        String email,
        Boolean activo,
        Boolean seAsocia,
        LocalDateTime fechaAltaManual
) {
}
