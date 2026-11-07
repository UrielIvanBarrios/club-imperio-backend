package com.clubimperio.gestionclub.seguridad.services;

import com.clubimperio.gestionclub.seguridad.dtos.CambioPasswordRequest;
import com.clubimperio.gestionclub.seguridad.dtos.LoginRequest;
import com.clubimperio.gestionclub.seguridad.dtos.LoginResponse;
import com.clubimperio.gestionclub.seguridad.dtos.UsuarioResponse;
import com.clubimperio.gestionclub.seguridad.entities.Usuario;
import com.clubimperio.gestionclub.seguridad.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request.email() == null || request.password() == null) {
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }

        // Mismo mensaje si el email no existe, si está inactivo o si la contraseña no coincide:
        // así no se le revela a un atacante qué emails están registrados.
        Usuario usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
                .filter(Usuario::getActivo)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException(CREDENCIALES_INVALIDAS));

        JwtService.TokenGenerado token = jwtService.generarToken(usuario);
        return new LoginResponse(token.token(), "Bearer", token.expiraEn(), UsuarioResponse.from(usuario));
    }

    @Transactional(readOnly = true)
    public Usuario perfil(UUID usuarioId) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        // El token sigue siendo válido hasta que vence, aunque un ADMIN haya desactivado al usuario.
        // Al menos /me lo detecta y responde 401, y el front cierra la sesión.
        if (!usuario.getActivo()) {
            throw new DisabledException("Tu usuario fue desactivado. Contactá a un administrador.");
        }
        return usuario;
    }

    @Transactional
    public void cambiarPassword(UUID usuarioId, CambioPasswordRequest request) {
        Usuario usuario = perfil(usuarioId);

        // 400 y no 401: el usuario SÍ está autenticado. Con un 401 el front cerraría la sesión.
        if (request.passwordActual() == null
                || !passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta.");
        }
        usuarioService.validarPassword(request.passwordNueva());
        if (request.passwordNueva().equals(request.passwordActual())) {
            throw new RuntimeException("La contraseña nueva debe ser distinta de la actual.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
    }
}
