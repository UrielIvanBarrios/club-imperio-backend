package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.entities.Membresia;
import com.clubimperio.gestionclub.membresia.entities.Persona;
import com.clubimperio.gestionclub.membresia.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;

    // Listar todas las personas
    @Transactional(readOnly = true)
    public List<Persona> listarConFiltros(Boolean activo, Boolean esSocio) {
        List<Persona> base = (activo != null)
                ? personaRepository.findByActivo(activo)
                : personaRepository.findAll();

        if (esSocio == null) {
            return base;
        }
        return base.stream()
                .filter(p -> p.isSocioActivo() == esSocio)
                .toList();
    }

    @Transactional(readOnly = true)
    public Persona buscarPorId(UUID id) {
        return personaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró al socio con el identificador técnico proporcionado."));
    }

    @Transactional(readOnly = true)
    public Persona buscarPorDni(String dni) {
        return personaRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("No existe socio con DNI: " + dni));
    }

    @Transactional
    public Persona guardar(Persona persona, boolean seAsocia, LocalDateTime fechaAltaManual) {
        // Misma regla de DNI para el alta manual y para la importación (que también pasa por acá)
        String dni = normalizarDni(persona.getDni());
        if (dni.isEmpty()) {
            throw new RuntimeException("El DNI es obligatorio.");
        }
        if (!esDniValido(dni)) {
            throw new RuntimeException(mensajeDniInvalido(persona.getDni()));
        }
        persona.setDni(dni);

        if(personaRepository.existsByDni(persona.getDni())) {
            throw new RuntimeException("Ya existe una persona con el dni: "+persona.getDni());
        }
        if (seAsocia) {
            LocalDateTime inicioMembresia = (fechaAltaManual != null)
                    ? fechaAltaManual
                    : LocalDateTime.now();

            Membresia primeraMembresia = Membresia.builder()
                    .persona(persona)
                    .fechaAlta(inicioMembresia)
                    .build();
            persona.getMembresias().add(primeraMembresia);
        }
        return personaRepository.save(persona);
    }

    @Transactional
    public Persona actualizar(UUID id, Persona datosNuevos) {
        Persona personaExistente = buscarPorId(id);

        // null = no vino en el body, no se toca. "" = borrar (opcionales) o 400 (obligatorios). Ver CamposTexto.
        if (datosNuevos.getNombre() != null) {
            personaExistente.setNombre(CamposTexto.obligatorio(datosNuevos.getNombre(), "El nombre no puede estar vacío."));
        }
        if (datosNuevos.getApellido() != null) {
            personaExistente.setApellido(CamposTexto.obligatorio(datosNuevos.getApellido(), "El apellido no puede estar vacío."));
        }
        if (datosNuevos.getEmail() != null) {
            personaExistente.setEmail(CamposTexto.opcional(datosNuevos.getEmail()));
        }
        if (datosNuevos.getTelefonoPrincipal() != null) {
            personaExistente.setTelefonoPrincipal(CamposTexto.opcional(datosNuevos.getTelefonoPrincipal()));
        }
        if (datosNuevos.getActivo() != null) {
            personaExistente.setActivo(datosNuevos.getActivo());
        }

        return personaRepository.save(personaExistente);
    }

    @Transactional
    public void eliminar(UUID id) {
        Persona persona = buscarPorId(id);
        persona.setActivo(false);
        personaRepository.save(persona);
    }

    @Transactional(readOnly = true)
    public long contarPorEstado(Boolean activo) {
        return personaRepository.countByActivo(activo);
    }

    @Transactional(readOnly = true)
    public long contarSociosPorEstado(Boolean activo, Boolean esSocio) {
        return listarConFiltros(activo, esSocio).size();
    }

    // ---------- Regla de DNI (única fuente: la usan el alta manual y la importación) ----------

    private static final Pattern DNI = Pattern.compile("^\\d{6,8}$");

    // "40.011.019" o "40 011 019" -> "40011019" (formatos habituales al tipear o en las planillas)
    public static String normalizarDni(String dni) {
        return dni == null ? "" : dni.trim().replaceAll("[.\\s-]", "");
    }

    // Recibe el DNI ya normalizado: de 6 a 8 dígitos, sin letras
    public static boolean esDniValido(String dniNormalizado) {
        return DNI.matcher(dniNormalizado).matches();
    }

    public static String mensajeDniInvalido(String dniOriginal) {
        return "DNI con formato inválido: '" + dniOriginal + "' (deben ser de 6 a 8 dígitos, sin letras).";
    }
}
