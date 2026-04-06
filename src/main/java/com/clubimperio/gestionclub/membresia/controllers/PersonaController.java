package com.clubimperio.gestionclub.membresia.controllers;

import com.clubimperio.gestionclub.membresia.entities.Persona;
import com.clubimperio.gestionclub.membresia.services.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {
    private final PersonaService personaService;

    //Obtener todas las personas
    @GetMapping
    /*public List<Persona> listarTodas() {
        return personaService.listarTodos();
    }*/
    public ResponseEntity<List<Persona>> listar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean esSocio
    ) {
        // Si no se pasan filtros, podrías decidir traer solo los activos por defecto
        // o usar una lógica que combine ambos parámetros.
        List<Persona> resultado = personaService.listarConFiltros(activo, esSocio);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/buscar")
    public ResponseEntity<Persona> buscarPorDni(@RequestParam String dni) {
        // Usamos el service que a su vez usa el repository.findByDni
        return ResponseEntity.ok(personaService.buscarPorDni(dni));
    }

    //Guardar una nueva persona (POST)
    @PostMapping
    public ResponseEntity<Persona> crear(@RequestBody Persona persona) {
        Persona nuevaPersona = personaService.guardar(persona);
        return new ResponseEntity<>(nuevaPersona, HttpStatus.CREATED);
    }

    //Buscar una persona especifica por DNI
    @GetMapping("/{id}")
    public ResponseEntity<Persona> obtenerUno(@PathVariable UUID id) {
        return ResponseEntity.ok(personaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Persona> actualizar(@PathVariable UUID id, @RequestBody Persona persona) {
        return ResponseEntity.ok(personaService.actualizar(id, persona));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        personaService.eliminar(id); // Deberás adaptar tu método eliminar para recibir UUID
        return ResponseEntity.noContent().build();
    }
}
