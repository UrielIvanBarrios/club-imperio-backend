package com.clubimperio.gestionclub.membresia.dtos;

import com.clubimperio.gestionclub.membresia.entities.Comision;
import com.clubimperio.gestionclub.membresia.entities.Inscripcion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta de inscripciones (y la inscripción anidada en cada asistencia). La persona va como PersonaResponse
// para cortar el ciclo Persona -> Membresia -> Persona. La comisión (con su actividad) no tiene relaciones
// hacia atrás, así que se devuelve tal cual.
public record InscripcionResponse(
        UUID inscripcionId,
        PersonaResponse persona,
        Comision comision,
        LocalDate fechaInscripcion,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDate fechaBaja
) {
    public static InscripcionResponse from(Inscripcion inscripcion) {
        return new InscripcionResponse(
                inscripcion.getInscripcionId(),
                PersonaResponse.from(inscripcion.getPersona()),
                inscripcion.getComision(),
                inscripcion.getFechaInscripcion(),
                inscripcion.getActivo(),
                inscripcion.getFechaCreacion(),
                inscripcion.getFechaBaja()
        );
    }
}
