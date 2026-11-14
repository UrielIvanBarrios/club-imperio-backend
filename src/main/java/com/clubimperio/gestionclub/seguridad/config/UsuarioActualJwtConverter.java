package com.clubimperio.gestionclub.seguridad.config;

import com.clubimperio.gestionclub.seguridad.entities.Usuario;
import com.clubimperio.gestionclub.seguridad.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Se ejecuta en cada request autenticada, después de que el JwtDecoder validó la firma y la expiración.
 * El token solo se usa para saber QUIÉN es (el "sub" = id del usuario). El estado y el rol se leen de la base,
 * así que desactivar a alguien o cambiarle el rol tiene efecto en su siguiente request, sin esperar a que el token venza.
 * El claim "rol" del token queda solo como dato informativo para el front: acá no se usa.
 *
 * Las excepciones que se lanzan son AuthenticationException: el filtro del resource server las atrapa y se las pasa
 * a ErroresSeguridadHandler, así que salen como 401 con el mismo formato JSON que el resto de los errores.
 */
@Component
@RequiredArgsConstructor
public class UsuarioActualJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = usuarioRepository.findById(idDelToken(jwt))
                // Usuario borrado de la base: mensaje genérico de sesión inválida (ver GlobalExceptionHandler)
                .orElseThrow(() -> new InvalidBearerTokenException("El usuario del token no existe."));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            // Mismo mensaje que GET /api/auth/me y que el login de un usuario desactivado
            throw new DisabledException("Tu usuario fue desactivado. Contactá a un administrador.");
        }

        // Autoridad a partir del rol ACTUAL de la base: ROLE_ADMIN / ROLE_USER, lo que espera hasRole(...)
        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        // Se sigue usando el Jwt como principal para que @AuthenticationPrincipal Jwt funcione igual en los controllers
        return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
    }

    private UUID idDelToken(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("El token no tiene un id de usuario válido.");
        }
    }
}
