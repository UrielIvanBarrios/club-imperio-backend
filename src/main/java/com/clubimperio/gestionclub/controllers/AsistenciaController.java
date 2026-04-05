package com.clubimperio.gestionclub.controllers;

import com.clubimperio.gestionclub.entities.Asistencia;
import com.clubimperio.gestionclub.services.AsistenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {
    private final AsistenciaService asistenciaService;

    @PostMapping
    public ResponseEntity<Asistencia> registrar(
            @RequestParam UUID inscripcionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "true") Boolean presente,
            @RequestParam(required = false) String observaciones
    ) {
        LocalDate fechaFinal = (fecha == null) ? LocalDate.now() : fecha;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asistenciaService.registrarAsistencia(inscripcionId, fechaFinal, presente, observaciones));
    }

    @GetMapping("/historial/{inscripcionId}")
    public ResponseEntity<List<Asistencia>> obtenerHistorial(@PathVariable UUID inscripcionId) {
        return ResponseEntity.ok(asistenciaService.historialPorInscripcion(inscripcionId));
    }
}
