package com.clubimperio.gestionclub.asistencia.dtos;

import java.time.LocalDate;

// Body de PUT /api/asistencias/{id}, para corregir una asistencia ya cargada.
// Misma regla que el resto de los PUT: ausente o null = no se modifica; observaciones "" = se borra (queda null).
// No incluye la inscripción: una asistencia no se "muda" a otro alumno (si se cargó mal, se corrige la otra).
public record AsistenciaUpdateRequest(
        LocalDate fecha,
        Boolean presente,
        String observaciones
) {
}
