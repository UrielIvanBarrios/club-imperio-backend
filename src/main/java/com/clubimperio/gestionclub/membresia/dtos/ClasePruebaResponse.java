package com.clubimperio.gestionclub.membresia.dtos;

import com.clubimperio.gestionclub.membresia.entities.Actividad;
import com.clubimperio.gestionclub.membresia.entities.ClasePrueba;

import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta de clases de prueba. La persona va como PersonaResponse (no la entidad): la entidad Persona tiene
// membresias -> cada Membresia apunta a su Persona, y serializarla directo entraba en recursión infinita
// cuando la persona era socia. La actividad se devuelve tal cual porque no tiene relaciones hacia atrás.
public record ClasePruebaResponse(
        UUID clasePruebaId,
        PersonaResponse persona,
        Actividad actividad,
        LocalDateTime fechaSolicitada,
        boolean asistio,
        Double costo,
        String observaciones,
        LocalDateTime fechaCreacion
) {
    public static ClasePruebaResponse from(ClasePrueba clase) {
        return new ClasePruebaResponse(
                clase.getClasePruebaId(),
                PersonaResponse.from(clase.getPersona()),
                clase.getActividad(),
                clase.getFechaSolicitada(),
                clase.isAsistio(),
                clase.getCosto(),
                clase.getObservaciones(),
                clase.getFechaCreacion()
        );
    }
}
