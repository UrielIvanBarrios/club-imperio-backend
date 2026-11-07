package com.clubimperio.gestionclub.membresia.dtos;

// Body del PUT /api/comisiones/{id}. Todos opcionales: solo se actualiza lo que viene distinto de null.
// No incluye la actividad (una comisión no se "muda" de actividad) ni "activa" (la baja tiene su propio DELETE).
public record ComisionUpdateRequest(
        String nombre,
        String horario,
        Integer cupoMaximo
) {
}
