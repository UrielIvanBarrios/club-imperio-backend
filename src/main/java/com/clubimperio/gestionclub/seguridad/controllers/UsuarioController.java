package com.clubimperio.gestionclub.seguridad.controllers;

import com.clubimperio.gestionclub.seguridad.dtos.UsuarioCreateRequest;
import com.clubimperio.gestionclub.seguridad.dtos.UsuarioResponse;
import com.clubimperio.gestionclub.seguridad.dtos.UsuarioUpdateRequest;
import com.clubimperio.gestionclub.seguridad.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Solo ADMIN: la restricción está centralizada en SecurityConfig ("/api/usuarios/**").
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos().stream().map(UsuarioResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerUno(@PathVariable UUID id) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorId(id)));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody UsuarioCreateRequest request) {
        return new ResponseEntity<>(UsuarioResponse.from(usuarioService.crear(request)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable UUID id,
                                                      @RequestBody UsuarioUpdateRequest request,
                                                      @AuthenticationPrincipal Jwt jwt) {
        UUID idLogueado = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.actualizar(id, request, idLogueado)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.desactivar(id, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
