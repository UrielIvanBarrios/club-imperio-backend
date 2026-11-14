package com.clubimperio.gestionclub.asistencia.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Body de POST /api/asistencias/clase: la asistencia de toda una clase (una comisión en una fecha).
// Se guarda todo o nada: si una sola fila es inválida, no se guarda ninguna.
public record AsistenciaClaseRequest(
        UUID comisionId,
        LocalDate fecha,
        List<Item> asistencias
) {
    // presente null = true (mismo default que el POST individual). observaciones: "" o null = sin observaciones.
    public record Item(
            UUID inscripcionId,
            Boolean presente,
            String observaciones
    ) {
    }
}
