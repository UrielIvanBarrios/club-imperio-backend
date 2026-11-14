package com.clubimperio.gestionclub.seguridad.services;

import com.clubimperio.gestionclub.seguridad.dtos.UsuarioCreateRequest;
import com.clubimperio.gestionclub.seguridad.dtos.UsuarioUpdateRequest;
import com.clubimperio.gestionclub.seguridad.entities.Rol;
import com.clubimperio.gestionclub.seguridad.entities.Usuario;
import com.clubimperio.gestionclub.seguridad.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int PASSWORD_MIN = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario."));
    }

    @Transactional
    public Usuario crear(UsuarioCreateRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("Ya existe un usuario con el email: " + email);
        }
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new RuntimeException("El nombre del usuario no puede estar vacío.");
        }
        if (request.rol() == null) {
            throw new RuntimeException("El rol es obligatorio (ADMIN o USER).");
        }
        validarPassword(request.password());

        Usuario usuario = Usuario.builder()
                .email(email)
                .nombre(request.nombre().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .build();
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(UUID id, UsuarioUpdateRequest request, UUID idUsuarioLogueado) {
        Usuario existente = buscarPorId(id);
        boolean esElMismo = existente.getUsuarioId().equals(idUsuarioLogueado);

        // Un ADMIN no puede quitarse a sí mismo el rol ni desactivarse: evita que el sistema
        // quede sin ningún ADMIN. Como quien edita a otro ADMIN sigue siendo ADMIN, siempre queda al menos uno.
        if (esElMismo && request.rol() != null && request.rol() != Rol.ADMIN) {
            throw new RuntimeException("No podés quitarte el rol de ADMIN a vos mismo.");
        }
        if (esElMismo && Boolean.FALSE.equals(request.activo())) {
            throw new RuntimeException("No podés desactivar tu propio usuario.");
        }
        // El reset sin contraseña actual es para ayudar a OTRO usuario que se la olvidó. Para la propia cuenta
        // se exige la actual: si el ADMIN deja la sesión abierta, nadie puede cambiarle la contraseña y quedarse con la cuenta.
        if (esElMismo && request.password() != null) {
            throw new RuntimeException("Para cambiar tu propia contraseña usá PUT /api/auth/me/password, que pide la contraseña actual.");
        }

        if (request.email() != null) {
            String email = normalizarEmail(request.email());
            if (!email.equals(existente.getEmail()) && usuarioRepository.existsByEmail(email)) {
                throw new RuntimeException("Ya existe un usuario con el email: " + email);
            }
            existente.setEmail(email);
        }
        if (request.nombre() != null && !request.nombre().isBlank()) {
            existente.setNombre(request.nombre().trim());
        }
        if (request.rol() != null) {
            existente.setRol(request.rol());
        }
        if (request.activo() != null) {
            existente.setActivo(request.activo());
        }
        if (request.password() != null) {
            validarPassword(request.password());
            existente.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return usuarioRepository.save(existente);
    }

    // Baja lógica, igual que en Personas y Actividades: se conserva el registro para auditoría.
    @Transactional
    public void desactivar(UUID id, UUID idUsuarioLogueado) {
        if (id.equals(idUsuarioLogueado)) {
            throw new RuntimeException("No podés desactivar tu propio usuario.");
        }
        Usuario usuario = buscarPorId(id);
        if (!usuario.getActivo()) {
            throw new RuntimeException("El usuario ya se encuentra desactivado.");
        }
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    public String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("El email es obligatorio.");
        }
        String normalizado = email.trim().toLowerCase();
        if (!EMAIL.matcher(normalizado).matches()) {
            throw new RuntimeException("El email no tiene un formato válido.");
        }
        return normalizado;
    }

    public void validarPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new RuntimeException("La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres.");
        }
    }
}
