package com.clubimperio.gestionclub.membresia.dtos;

// Body del PUT /api/comisiones/{id}. Todos opcionales: solo se actualiza lo que viene distinto de null.
// No incluye la actividad (una comisión no se "muda" de actividad) ni "activa" (la baja tiene su propio DELETE).
// sinTope = true quita el cupo máximo (queda null = sin tope). Hace falta un campo aparte porque
// cupoMaximo: null ya significa "no tocar el cupo", y no se puede usar el mismo valor para dos cosas.
public record ComisionUpdateRequest(
        String nombre,
        String horario,
        Integer cupoMaximo,
        Boolean sinTope
) {
}
