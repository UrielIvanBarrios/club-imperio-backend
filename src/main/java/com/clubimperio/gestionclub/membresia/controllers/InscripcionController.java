package com.clubimperio.gestionclub.membresia.controllers;

import com.clubimperio.gestionclub.membresia.dtos.InscripcionResponse;
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
    public ResponseEntity<InscripcionResponse> inscribir(
            @RequestParam String dni,
            @RequestParam UUID comisionId
    ){
        return ResponseEntity.status(201).body(InscripcionResponse.from(inscripcionService.inscribirSocio(dni, comisionId)));
    }

    @GetMapping("/socio/{dni}")
    public ResponseEntity<List<InscripcionResponse>> listarPorSocio(@PathVariable String dni){
        return ResponseEntity.ok(inscripcionService.listarPorSocio(dni).stream().map(InscripcionResponse::from).toList());
    }

    @GetMapping("/comision/{comisionId}")
    public ResponseEntity<List<InscripcionResponse>> listarPorComision(@PathVariable UUID comisionId) {
        return ResponseEntity.ok(inscripcionService.listarPorComision(comisionId).stream().map(InscripcionResponse::from).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable UUID id) {
        inscripcionService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

}
