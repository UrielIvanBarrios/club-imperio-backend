package com.clubimperio.gestionclub.controllers;

import com.clubimperio.gestionclub.entities.Comision;
import com.clubimperio.gestionclub.services.ComisionService;
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

    @GetMapping("/actividad/{actividadId}")
    public ResponseEntity<List<Comision>> listarPorActividad(@PathVariable UUID actividadId){
        return ResponseEntity.ok(comisionService.listarPorActividad(actividadId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comision> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(comisionService.buscarPorId(id));
    }
}
