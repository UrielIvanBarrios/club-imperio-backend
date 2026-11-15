package com.clubimperio.gestionclub.asistencia.dtos;

import com.clubimperio.gestionclub.asistencia.entities.Asistencia;
import com.clubimperio.gestionclub.membresia.dtos.InscripcionResponse;

import java.time.LocalDate;
import java.util.UUID;

// Respuesta de asistencias. La inscripción va como InscripcionResponse, que a su vez trae la persona como
// PersonaResponse: así no se serializa nunca la entidad Persona (ciclo Persona -> Membresia -> Persona).
public record AsistenciaResponse(
        UUID asistenciaId,
        InscripcionResponse inscripcion,
        LocalDate fecha,
        boolean presente,
        String observaciones
) {
    public static AsistenciaResponse from(Asistencia asistencia) {
        return new AsistenciaResponse(
                asistencia.getAsistenciaId(),
                InscripcionResponse.from(asistencia.getInscripcion()),
                asistencia.getFecha(),
                asistencia.isPresente(),
                asistencia.getObservaciones()
        );
    }
}
