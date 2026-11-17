package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.dtos.InscripcionListadoResponse;
import com.clubimperio.gestionclub.membresia.entities.Comision;
import com.clubimperio.gestionclub.membresia.entities.Inscripcion;
import com.clubimperio.gestionclub.membresia.entities.Persona;
import com.clubimperio.gestionclub.membresia.repositories.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InscripcionService {
    private final InscripcionRepository inscripcionRepository;
    private final PersonaService personaService;
    private final ComisionService comisionService;

    @Transactional
    public Inscripcion inscribirSocio(String dni, UUID comisionId){
        Persona persona = personaService.buscarPorDni(dni);
        if(persona.getActivo() == null || !persona.getActivo()){
            throw new RuntimeException("La persona seleccionada no esta activa.");
        }
        // Bloquea la comisión hasta el final de esta transacción. Así, dos inscripciones simultáneas a la misma
        // comisión se hacen de a una: la segunda cuenta los inscriptos recién cuando la primera ya confirmó,
        // y el cupo no se puede superar. También evita que la misma persona quede inscripta dos veces por un doble clic.
        // No se exige membresía activa: es intencional (un no socio puede anotarse a una actividad, ver api-spec.md).
        Comision comision = comisionService.buscarPorIdConBloqueo(comisionId);
        if(comision.getActiva() == null || !comision.getActiva()){
            throw new RuntimeException("La comision seleccionada no esta activa.");
        }

        if (inscripcionRepository.existsByPersona_DniAndComision_ComisionIdAndActivoTrue(dni, comisionId)) {
            throw new RuntimeException("El socio ya está inscripto en esta comisión.");
        }

        if (comision.getCupoMaximo() != null) {
            long actuales = inscripcionRepository.countByComision_ComisionIdAndActivoTrue(comisionId);
            if (actuales >= comision.getCupoMaximo()) {
                throw new RuntimeException("Cupo agotado para la comisión: " + comision.getNombre());
            }
        }

        Inscripcion nueva = Inscripcion.builder()
                .persona(persona)
                .comision(comision)
                .fechaInscripcion(LocalDate.now())
                .activo(true)
                .build();

        return inscripcionRepository.save(nueva);

    }

    @Transactional(readOnly = true)
    public List<Inscripcion> listarPorSocio(String dni) {
        return inscripcionRepository.findByPersona_DniAndActivoTrue(dni);
    }

    @Transactional(readOnly = true)
    public List<Inscripcion> listarPorComision(UUID comisionId) {
        return inscripcionRepository.findByComision_ComisionIdAndActivoTrue(comisionId);
    }

    // Listado con filtros opcionales para la tabla de inscripciones. La consulta se resuelve entera en la base
    // (ver InscripcionRepositoryCustomImpl); acá solo se validan y se preparan los parámetros.
    @Transactional(readOnly = true)
    public List<InscripcionListadoResponse> listar(UUID actividadId, UUID comisionId, String q, String estado) {
        if (actividadId != null && comisionId != null) {
            Comision comision = comisionService.buscarPorId(comisionId);
            if (!comision.getActividad().getActividadId().equals(actividadId)) {
                throw new RuntimeException("La comisión " + comision.getNombre() + " no pertenece a la actividad indicada.");
            }
        }
        return inscripcionRepository.listarConFiltros(actividadId, comisionId, filtroActivo(estado), patronBusqueda(q));
    }

    // estado: "activas" (default), "bajas" o "todas" -> true, false o null (sin filtro)
    private Boolean filtroActivo(String estado) {
        if (estado == null || estado.isBlank()) {
            return true;
        }
        return switch (estado.trim().toLowerCase()) {
            case "activas" -> true;
            case "bajas" -> false;
            case "todas" -> null;
            default -> throw new RuntimeException("El estado debe ser 'activas', 'bajas' o 'todas'.");
        };
    }

    // Convierte el texto buscado en un patrón de LIKE que se compara contra los datos también sin tildes ni mayúsculas.
    // Devuelve null si no hay nada para buscar (no se filtra).
    private String patronBusqueda(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String texto = q.trim();
        // DNI escrito con puntos (40.011.019): en la base se guarda sin puntos
        if (texto.replace(".", "").matches("\\d+")) {
            texto = texto.replace(".", "");
        }
        // Saca las tildes: NFD separa "á" en "a" + tilde y la regex borra las tildes sueltas. La ñ también queda como n,
        // igual que en la consulta. Después se pasan a minúsculas y se juntan los espacios repetidos.
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("\\s+", " ");
        // Si el usuario escribe % o _, se buscan literalmente y no como comodines de LIKE
        texto = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + texto + "%";
    }

    @Transactional(readOnly = true)
    public Inscripcion buscarPorId(UUID id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada."));
    }

    @Transactional
    public void darDeBaja(UUID id) {
        Inscripcion inscripcion = buscarPorId(id);
        inscripcion.setActivo(false);
        inscripcion.setFechaBaja(LocalDate.now());
        inscripcionRepository.save(inscripcion);
    }
}



