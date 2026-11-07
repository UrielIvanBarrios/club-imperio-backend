package com.clubimperio.gestionclub.seguridad.controllers;

import com.clubimperio.gestionclub.seguridad.dtos.CambioPasswordRequest;
import com.clubimperio.gestionclub.seguridad.dtos.LoginRequest;
import com.clubimperio.gestionclub.seguridad.dtos.LoginResponse;
import com.clubimperio.gestionclub.seguridad.dtos.UsuarioResponse;
import com.clubimperio.gestionclub.seguridad.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // Público (ver SecurityConfig). Devuelve el token que el front guarda y manda en cada request.
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // @AuthenticationPrincipal inyecta el JWT ya validado; su "subject" es el id del usuario.
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(UsuarioResponse.from(authService.perfil(UUID.fromString(jwt.getSubject()))));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal Jwt jwt,
                                                @RequestBody CambioPasswordRequest request) {
        authService.cambiarPassword(UUID.fromString(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }
}
