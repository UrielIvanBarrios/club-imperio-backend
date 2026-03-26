package com.clubimperio.gestionclub.controllers;

import com.clubimperio.gestionclub.entities.Actividad;
import com.clubimperio.gestionclub.services.ActividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/actividades")
@RequiredArgsConstructor
public class ActividadController {
    private final ActividadService actividadService;

    @GetMapping
    public ResponseEntity<List<Actividad>> listar() {
        return ResponseEntity.ok(actividadService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<Actividad> crear(@RequestBody Actividad actividad) {
        return ResponseEntity.status(201).body(actividadService.guardar(actividad));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Actividad> obtenerUna(@PathVariable UUID id) {
        return ResponseEntity.ok(actividadService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<Actividad> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(actividadService.buscarPorNombre(nombre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Actividad> actualizar(@PathVariable UUID id, @RequestBody Actividad actividad) {
        return ResponseEntity.ok(actividadService.actualizar(id, actividad));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        actividadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
