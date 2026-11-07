package com.clubimperio.gestionclub.seguridad.config;

import com.clubimperio.gestionclub.seguridad.dtos.UsuarioCreateRequest;
import com.clubimperio.gestionclub.seguridad.entities.Rol;
import com.clubimperio.gestionclub.seguridad.repositories.UsuarioRepository;
import com.clubimperio.gestionclub.seguridad.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Problema del "huevo y la gallina": el ABM de usuarios es solo para ADMIN, así que con la base vacía
 * nadie podría crear el primer usuario. Al arrancar, si la tabla usuarios está vacía,
 * se crea un ADMIN con los datos de application.properties. Si ya hay usuarios, no hace nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicialRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Value("${app.admin.nombre:Administrador}")
    private String nombre;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No hay usuarios y faltan app.admin.email / app.admin.password: nadie va a poder iniciar sesión.");
            return;
        }
        // Pasa por el service para aplicar las mismas validaciones (email válido, contraseña de 8+ caracteres)
        usuarioService.crear(new UsuarioCreateRequest(email, nombre, password, Rol.ADMIN));
        log.info("Usuario ADMIN inicial creado: {}", email);
    }
}
