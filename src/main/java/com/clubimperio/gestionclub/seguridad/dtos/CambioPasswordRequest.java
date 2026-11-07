package com.clubimperio.gestionclub.seguridad.dtos;

public record CambioPasswordRequest(
        String passwordActual,
        String passwordNueva
) {
}
