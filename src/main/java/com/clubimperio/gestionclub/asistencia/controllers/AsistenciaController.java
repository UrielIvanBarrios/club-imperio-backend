package com.clubimperio.gestionclub.asistencia.controllers;

import com.clubimperio.gestionclub.asistencia.dtos.AsistenciaClaseRequest;
import com.clubimperio.gestionclub.asistencia.dtos.AsistenciaResponse;
import com.clubimperio.gestionclub.asistencia.dtos.AsistenciaUpdateRequest;
import com.clubimperio.gestionclub.asistencia.entities.Asistencia;
import com.clubimperio.gestionclub.asistencia.services.AsistenciaService;
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
    public ResponseEntity<AsistenciaResponse> registrar(
            @RequestParam UUID inscripcionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "true") Boolean presente,
            @RequestParam(required = false) String observaciones
    ) {
        LocalDate fechaFinal = (fecha == null) ? LocalDate.now() : fecha;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AsistenciaResponse.from(asistenciaService.registrarAsistencia(inscripcionId, fechaFinal, presente, observaciones)));
    }

    // Asistencia de toda una clase en una sola transacción (todo o nada)
    @PostMapping("/clase")
    public ResponseEntity<List<AsistenciaResponse>> registrarClase(@RequestBody AsistenciaClaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aRespuesta(asistenciaService.registrarClase(request)));
    }

    // Lo cargado para una clase: GET /api/asistencias/comision/{id}?fecha=2026-11-14
    @GetMapping("/comision/{comisionId}")
    public ResponseEntity<List<AsistenciaResponse>> listarPorComisionYFecha(
            @PathVariable UUID comisionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(aRespuesta(asistenciaService.listarPorComisionYFecha(comisionId, fecha)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AsistenciaResponse> corregir(@PathVariable UUID id, @RequestBody AsistenciaUpdateRequest request) {
        return ResponseEntity.ok(AsistenciaResponse.from(asistenciaService.corregir(id, request)));
    }

    @GetMapping("/historial/{inscripcionId}")
    public ResponseEntity<List<AsistenciaResponse>> obtenerHistorial(@PathVariable UUID inscripcionId) {
        return ResponseEntity.ok(aRespuesta(asistenciaService.historialPorInscripcion(inscripcionId)));
    }

    // Nunca se devuelve la entidad: trae una Persona adentro y su lista de membresías vuelve a la Persona (recursión)
    private static List<AsistenciaResponse> aRespuesta(List<Asistencia> asistencias) {
        return asistencias.stream().map(AsistenciaResponse::from).toList();
    }
}
