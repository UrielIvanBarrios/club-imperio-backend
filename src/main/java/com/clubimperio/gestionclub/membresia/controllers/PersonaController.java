package com.clubimperio.gestionclub.membresia.controllers;

import com.clubimperio.gestionclub.membresia.dtos.PersonaCreateRequest;
import com.clubimperio.gestionclub.membresia.dtos.PersonaResponse;
import com.clubimperio.gestionclub.membresia.dtos.PersonaUpdateRequest;
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
@CrossOrigin(origins = "*")
@RequiredArgsConstructor

public class PersonaController {
    private final PersonaService personaService;

    //Obtener todas las personas
    @GetMapping
    public ResponseEntity<List<PersonaResponse>> listar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean esSocio
    ) {
        // Si no se pasan filtros, podrías decidir traer solo los activos por defecto
        // o usar una lógica que combine ambos parámetros.
        List<PersonaResponse> resultado = personaService.listarConFiltros(activo, esSocio)
                .stream()
                .map(PersonaResponse::from)
                .toList();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/buscar")
    public ResponseEntity<PersonaResponse> buscarPorDni(@RequestParam String dni) {
        // Usamos el service que a su vez usa el repository.findByDni
        return ResponseEntity.ok(PersonaResponse.from(personaService.buscarPorDni(dni)));
    }

    //Guardar una nueva persona (POST)
    @PostMapping
    public ResponseEntity<PersonaResponse> crear(@RequestBody PersonaCreateRequest request) {
        Persona persona = Persona.builder()
                .dni(request.dni())
                .nombre(request.nombre())
                .apellido(request.apellido())
                .fechaNacimiento(request.fechaNacimiento())
                .telefonoPrincipal(request.telefonoPrincipal())
                .email(request.email())
                .activo(request.activo() != null ? request.activo() : true)
                .build();
        Persona nuevaPersona = personaService.guardar(persona, Boolean.TRUE.equals(request.seAsocia()), request.fechaAltaManual());
        return new ResponseEntity<>(PersonaResponse.from(nuevaPersona), HttpStatus.CREATED);
    }

    //Buscar una persona especifica por DNI
    @GetMapping("/{id}")
    public ResponseEntity<PersonaResponse> obtenerUno(@PathVariable UUID id) {
        return ResponseEntity.ok(PersonaResponse.from(personaService.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonaResponse> actualizar(@PathVariable UUID id, @RequestBody PersonaUpdateRequest request) {
        Persona datosNuevos = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .email(request.email())
                .telefonoPrincipal(request.telefonoPrincipal())
                .activo(request.activo())
                .build();
        return ResponseEntity.ok(PersonaResponse.from(personaService.actualizar(id, datosNuevos)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/count")
    public ResponseEntity<ConteoResponse> obtenerConteo(
            @RequestParam Boolean activo,
            @RequestParam(required = false) Boolean esSocio
    ) {
        long cantidad;
        if (esSocio != null) {
            cantidad = personaService.contarSociosPorEstado(activo, esSocio);
        } else {
            cantidad = personaService.contarPorEstado(activo);
        }

        // Devolvemos el objeto record. Jackson lo convierte a {"total": X} automáticamente.
        return ResponseEntity.ok(new ConteoResponse(cantidad));
    }

    private record ConteoResponse(long total) {}
}
