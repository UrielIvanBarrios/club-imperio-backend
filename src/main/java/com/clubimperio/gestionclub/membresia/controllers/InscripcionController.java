package com.clubimperio.gestionclub.membresia.controllers;

import com.clubimperio.gestionclub.membresia.entities.Inscripcion;
import com.clubimperio.gestionclub.membresia.services.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inscripciones")
@RequiredArgsConstructor
public class InscripcionController {
    private final InscripcionService inscripcionService;

    @PostMapping
    public ResponseEntity<Inscripcion> inscribir(
            @RequestParam String dni,
            @RequestParam UUID comisionId
    ){
        return ResponseEntity.status(201).body(inscripcionService.inscribirSocio(dni, comisionId));
    }

    @GetMapping("/socio/{dni}")
    public ResponseEntity<List<Inscripcion>> listarPorSocio(@PathVariable String dni){
        return ResponseEntity.ok(inscripcionService.listarPorSocio(dni));
    }

    @GetMapping("/comision/{comisionId}")
    public ResponseEntity<List<Inscripcion>> listarPorComision(@PathVariable UUID comisionId) {
        return ResponseEntity.ok(inscripcionService.listarPorComision(comisionId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable UUID id) {
        inscripcionService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

}
