package com.clubimperio.gestionclub.membresia.dtos;

import com.clubimperio.gestionclub.membresia.entities.Membresia;

import java.time.LocalDateTime;
import java.util.UUID;

public record MembresiaResponse(
        UUID id,
        LocalDateTime fechaAlta,
        LocalDateTime fechaBaja,
        String motivoBaja
) {
    public static MembresiaResponse from(Membresia membresia) {
        return new MembresiaResponse(
                membresia.getId(),
                membresia.getFechaAlta(),
                membresia.getFechaBaja(),
                membresia.getMotivoBaja()
        );
    }
}
