package com.clubimperio.gestionclub.controllers;

import com.clubimperio.gestionclub.entities.ClasePrueba;
import com.clubimperio.gestionclub.services.ClasePruebaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clases-prueba")
@RequiredArgsConstructor
public class ClasePruebaController {
    private final ClasePruebaService clasePruebaService;

    @PostMapping("/registrar")
    public ResponseEntity<ClasePrueba> registrar (
            @RequestParam String dni,
            @RequestParam UUID actividadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fecha
    ){
        ClasePrueba nuevaClase = clasePruebaService.registrarClasePrueba(dni, actividadId, fecha);
        return ResponseEntity.status(201).body(nuevaClase);
    }
    @GetMapping
    public ResponseEntity<List<ClasePrueba>> listarTodas(){
        return ResponseEntity.ok(clasePruebaService.listarTodas());
    }

    @PatchMapping("/{id}/asistencia")
    public ResponseEntity<Void> marcarAsistencia(
            @PathVariable UUID id,
            @RequestParam boolean asistio,
            @RequestParam(required = false) String observaciones
    ) {
        clasePruebaService.marcarAsistencia(id, asistio, observaciones);
        return ResponseEntity.noContent().build();
    }
}
