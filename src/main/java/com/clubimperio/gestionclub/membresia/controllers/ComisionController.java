package com.clubimperio.gestionclub.membresia.controllers;

import com.clubimperio.gestionclub.membresia.dtos.ComisionUpdateRequest;
import com.clubimperio.gestionclub.membresia.entities.Comision;
import com.clubimperio.gestionclub.membresia.services.ComisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comisiones")
@RequiredArgsConstructor
public class ComisionController {
    private final ComisionService comisionService;

    @PostMapping
    public ResponseEntity<Comision> crear(
            @RequestParam UUID actividadId,
            @RequestParam String nombre,
            @RequestParam String horario,
            @RequestParam(required = false) Integer cupo
    ){
        Comision nueva = comisionService.crearComision(actividadId, nombre, horario, cupo);
        return ResponseEntity.status(201).body(nueva);
    }

    // Por defecto solo las activas (lo que necesita el listado del front). ?incluirInactivas=true trae todas.
    @GetMapping("/actividad/{actividadId}")
    public ResponseEntity<List<Comision>> listarPorActividad(
            @PathVariable UUID actividadId,
            @RequestParam(defaultValue = "false") boolean incluirInactivas
    ){
        return ResponseEntity.ok(comisionService.listarPorActividad(actividadId, incluirInactivas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comision> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(comisionService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comision> actualizar(@PathVariable UUID id, @RequestBody ComisionUpdateRequest request) {
        return ResponseEntity.ok(comisionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        comisionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
