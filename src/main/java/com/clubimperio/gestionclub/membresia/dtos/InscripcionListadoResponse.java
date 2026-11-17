package com.clubimperio.gestionclub.membresia.dtos;

import java.time.LocalDate;
import java.util.UUID;

// Fila del listado de inscripciones (GET /api/inscripciones). Trae solo lo que muestra la tabla del front:
// a diferencia de InscripcionResponse, no lleva las membresías de la persona ni la comisión completa.
// Se arma directo en la consulta (SELECT new ...), así que no se cargan entidades ni hay N+1.
public record InscripcionListadoResponse(
        UUID inscripcionId,
        LocalDate fechaInscripcion,
        Boolean activo,
        LocalDate fechaBaja,
        PersonaResumen persona,
        ComisionResumen comision,
        ActividadResumen actividad
) {
    public record PersonaResumen(UUID personaId, String dni, String nombre, String apellido, Boolean esSocioActivo) {
    }

    public record ComisionResumen(UUID comisionId, String nombre, String horario) {
    }

    public record ActividadResumen(UUID actividadId, String nombre) {
    }

    // Constructor "plano" para el SELECT new de JPQL: JPQL no puede construir objetos anidados,
    // así que recibe las columnas sueltas y acá se agrupan en persona, comisión y actividad.
    public InscripcionListadoResponse(
            UUID inscripcionId, LocalDate fechaInscripcion, Boolean activo, LocalDate fechaBaja,
            UUID personaId, String dni, String nombre, String apellido, Boolean esSocioActivo,
            UUID comisionId, String comisionNombre, String horario,
            UUID actividadId, String actividadNombre
    ) {
        this(inscripcionId, fechaInscripcion, activo, fechaBaja,
                new PersonaResumen(personaId, dni, nombre, apellido, esSocioActivo),
                new ComisionResumen(comisionId, comisionNombre, horario),
                new ActividadResumen(actividadId, actividadNombre));
    }
}
