package com.clubimperio.gestionclub.seguridad.services;

import com.clubimperio.gestionclub.seguridad.config.JwtConfig;
import com.clubimperio.gestionclub.seguridad.entities.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expiracionHoras;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiracion-horas:8}") long expiracionHoras) {
        this.jwtEncoder = jwtEncoder;
        this.expiracionHoras = expiracionHoras;
    }

    public TokenGenerado generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plus(expiracionHoras, ChronoUnit.HOURS);

        // El token va firmado, pero NO cifrado: cualquiera puede leer su contenido.
        // Por eso solo lleva datos no sensibles (nunca la contraseña).
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("club-imperio")
                .subject(usuario.getUsuarioId().toString())
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .claim("email", usuario.getEmail())
                .claim(JwtConfig.CLAIM_ROL, usuario.getRol().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenGenerado(token, expiraEn);
    }

    public record TokenGenerado(String token, Instant expiraEn) {}
}
