package com.clubimperio.gestionclub.seguridad.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Los rechazos de Spring Security (sin token, token vencido, rol insuficiente) ocurren en los filtros,
 * ANTES de llegar a un controller, así que el @RestControllerAdvice no los ve.
 * Este handler se los reenvía al GlobalExceptionHandler para que TODOS los errores salgan con el mismo formato JSON.
 */
@Component
public class ErroresSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public ErroresSeguridadHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    // 401: no hay token o es inválido/vencido
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        resolver.resolveException(request, response, null, ex);
    }

    // 403: hay token válido pero el rol no alcanza
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
