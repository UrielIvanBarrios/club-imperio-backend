package com.clubimperio.gestionclub.seguridad.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 7: ya no existe WebSecurityConfigurerAdapter.
 * La configuración se declara como un bean SecurityFilterChain.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ErroresSeguridadHandler erroresSeguridadHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Usa el bean corsConfigurationSource de abajo. Va dentro de Security (y no solo en Spring MVC)
                // para que el preflight OPTIONS del navegador se responda ANTES de exigir token:
                // el navegador nunca manda el header Authorization en un preflight.
                .cors(Customizer.withDefaults())
                // CSRF protege sesiones basadas en cookies. Acá el token viaja en el header Authorization,
                // que el navegador no adjunta solo, así que CSRF no aplica.
                .csrf(csrf -> csrf.disable())
                // Stateless: el servidor no guarda sesión, cada request se autentica con su JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos: login, Swagger y la página de error interna de Spring
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        // ABM de usuarios: solo ADMIN
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        // Todo lo demás: cualquier usuario logueado (ADMIN o USER)
                        .anyRequest().authenticated()
                )
                // Valida el "Authorization: Bearer <token>" de cada request con el JwtDecoder de JwtConfig
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(erroresSeguridadHandler)
                        .accessDeniedHandler(erroresSeguridadHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(erroresSeguridadHandler)
                        .accessDeniedHandler(erroresSeguridadHandler)
                );

        return http.build();
    }

    /**
     * CORS: el navegador bloquea las llamadas de un origen (el front en :5173) a otro (el back en :8080)
     * salvo que el back las autorice explícitamente. Solo se habilitan los orígenes de app.cors.allowed-origins,
     * nunca "*".
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> origenesPermitidos) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // false: el token viaja en el header Authorization, no en cookies, así que no hacen falta credenciales
        config.setAllowCredentials(false);
        // El navegador cachea la respuesta del preflight 1 hora y no lo repite en cada request
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // BCrypt: hash lento y con salt aleatorio. Dos usuarios con la misma contraseña tienen hashes distintos.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
