package com.clubimperio.gestionclub.controllers;

import com.clubimperio.gestionclub.entities.Persona;
import com.clubimperio.gestionclub.services.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {
    private final PersonaService personaService;

    //Obtener todas las personas
    @GetMapping
    public List<Persona> listarTodas() {
        return personaService.listarTodos();
    }

    //Guardar una nueva persona (POST)
    @PostMapping
    public ResponseEntity<Persona> crear(@RequestBody Persona persona) {
        Persona nuevaPersona = personaService.guardar(persona);
        return new ResponseEntity<>(nuevaPersona, HttpStatus.CREATED);
    }

    //Buscar una persona especifica por DNI
    @GetMapping("/{dni}")
    public ResponseEntity<Persona> buscarPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(personaService.buscarPorDni(dni));
    }
}
