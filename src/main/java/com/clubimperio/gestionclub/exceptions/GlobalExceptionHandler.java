package com.clubimperio.gestionclub.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Las excepciones de Spring Security también son RuntimeException. Sin estos handlers más específicos,
    // caerían en el de abajo y saldrían como 400, y el front no podría distinguir "sesión vencida" de "dato inválido".
    // Spring elige siempre el handler del tipo más específico, por eso estos ganan sobre RuntimeException.

    // 401: login fallido, sin token, token inválido o vencido, usuario desactivado
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        // Los mensajes de login y de usuario desactivado los escribimos nosotros; el resto (detalles técnicos
        // del token) se reemplaza por uno genérico para no exponer información interna.
        String mensaje = (ex instanceof BadCredentialsException || ex instanceof DisabledException)
                ? ex.getMessage()
                : "Sesión inválida o vencida. Iniciá sesión nuevamente.";
        return construirRespuesta(HttpStatus.UNAUTHORIZED, mensaje);
    }

    // 403: el usuario está logueado pero su rol no le permite la acción
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción.");
    }

    // 400: errores de negocio lanzados por los services
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", mensaje);
        body.put("status", status.value());

        return new ResponseEntity<>(body, status);
    }
}
