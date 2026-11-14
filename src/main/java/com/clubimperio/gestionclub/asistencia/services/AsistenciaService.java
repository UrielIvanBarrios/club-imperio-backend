package com.clubimperio.gestionclub.asistencia.services;

import com.clubimperio.gestionclub.asistencia.dtos.AsistenciaClaseRequest;
import com.clubimperio.gestionclub.asistencia.dtos.AsistenciaUpdateRequest;
import com.clubimperio.gestionclub.asistencia.entities.Asistencia;
import com.clubimperio.gestionclub.asistencia.repositories.AsistenciaRepository;
import com.clubimperio.gestionclub.membresia.entities.Comision;
import com.clubimperio.gestionclub.membresia.entities.Inscripcion;
import com.clubimperio.gestionclub.membresia.services.CamposTexto;
import com.clubimperio.gestionclub.membresia.services.ComisionService;
import com.clubimperio.gestionclub.membresia.services.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AsistenciaService {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionService inscripcionService;
    private final ComisionService comisionService;

    @Transactional
    public Asistencia registrarAsistencia(UUID inscripcionId, LocalDate fecha, Boolean presente, String observaciones) {

        Inscripcion inscripcion = inscripcionService.buscarPorId(inscripcionId);

        if (inscripcion.getActivo() == null || !inscripcion.getActivo()) {
            throw new RuntimeException("No se puede registrar asistencia de una inscripción dada de baja.");
        }
        validarFecha(inscripcion, fecha);

        if (asistenciaRepository.existsByInscripcion_InscripcionIdAndFecha(inscripcionId, fecha)) {
            throw new RuntimeException(mensajeDuplicada(fecha));
        }

        Asistencia nuevaAsistencia = Asistencia.builder()
                .inscripcion(inscripcion)
                .fecha(fecha)
                .presente(presente)
                .observaciones(observaciones)
                .build();

        return guardarTraduciendoDuplicado(() -> asistenciaRepository.saveAndFlush(nuevaAsistencia), fecha);
    }

    /**
     * Asistencia de toda una clase (una comisión en una fecha) en una sola transacción: todo o nada.
     * Primero valida TODAS las filas y junta todos los errores, así el front puede mostrar todo lo que está mal
     * de una vez. Si hay alguno, no se guarda nada. Si dos personas cargan la misma clase a la vez, la restricción
     * única de la base frena a la segunda y su transacción entera se revierte.
     */
    @Transactional
    public List<Asistencia> registrarClase(AsistenciaClaseRequest request) {
        if (request.comisionId() == null) {
            throw new RuntimeException("La comisión es obligatoria.");
        }
        if (request.fecha() == null) {
            throw new RuntimeException("La fecha de la clase es obligatoria.");
        }
        if (request.asistencias() == null || request.asistencias().isEmpty()) {
            throw new RuntimeException("La lista de asistencias no puede estar vacía.");
        }
        Comision comision = comisionService.buscarPorId(request.comisionId());
        if (!Boolean.TRUE.equals(comision.getActiva())) {
            throw new RuntimeException("No se puede cargar asistencia en una comisión dada de baja.");
        }
        LocalDate fecha = request.fecha();
        // Las reglas que no dependen de la inscripción se chequean una sola vez
        if (fecha.isAfter(LocalDate.now())) {
            throw new RuntimeException(mensajeFechaFutura(fecha));
        }

        List<String> errores = new ArrayList<>();
        List<Asistencia> nuevas = new ArrayList<>();
        Set<UUID> vistas = new HashSet<>();

        for (int i = 0; i < request.asistencias().size(); i++) {
            AsistenciaClaseRequest.Item item = request.asistencias().get(i);
            String fila = "Fila " + (i + 1);
            if (item == null || item.inscripcionId() == null) {
                errores.add(fila + ": falta la inscripción.");
                continue;
            }
            if (!vistas.add(item.inscripcionId())) {
                errores.add(fila + ": la inscripción está repetida en la lista.");
                continue;
            }
            Inscripcion inscripcion;
            try {
                inscripcion = inscripcionService.buscarPorId(item.inscripcionId());
            } catch (RuntimeException e) {
                errores.add(fila + ": la inscripción no existe.");
                continue;
            }
            String quien = fila + " (" + inscripcion.getPersona().getApellido() + ", " + inscripcion.getPersona().getNombre() + ")";
            if (!inscripcion.getComision().getComisionId().equals(comision.getComisionId())) {
                errores.add(quien + ": la inscripción es de otra comisión.");
                continue;
            }
            if (!Boolean.TRUE.equals(inscripcion.getActivo())) {
                errores.add(quien + ": la inscripción está dada de baja.");
                continue;
            }
            try {
                validarFecha(inscripcion, fecha);
            } catch (RuntimeException e) {
                errores.add(quien + ": " + e.getMessage());
                continue;
            }
            if (asistenciaRepository.existsByInscripcion_InscripcionIdAndFecha(inscripcion.getInscripcionId(), fecha)) {
                errores.add(quien + ": " + mensajeDuplicada(fecha));
                continue;
            }
            nuevas.add(Asistencia.builder()
                    .inscripcion(inscripcion)
                    .fecha(fecha)
                    .presente(item.presente() == null || item.presente())
                    .observaciones(item.observaciones() == null ? null : CamposTexto.opcional(item.observaciones()))
                    .build());
        }

        if (!errores.isEmpty()) {
            throw new RuntimeException("No se guardó ninguna asistencia. " + String.join(" ", errores));
        }
        // saveAllAndFlush: los INSERT se ejecutan acá adentro, así una violación de la restricción única
        // se puede traducir a un 400 claro y revierte la transacción entera
        return guardarTraduciendoDuplicado(() -> asistenciaRepository.saveAllAndFlush(nuevas), fecha);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> listarPorComisionYFecha(UUID comisionId, LocalDate fecha) {
        comisionService.buscarPorId(comisionId); // 400 claro si la comisión no existe, en vez de una lista vacía
        return asistenciaRepository
                .findByInscripcion_Comision_ComisionIdAndFechaOrderByInscripcion_Persona_ApellidoAscInscripcion_Persona_NombreAsc(
                        comisionId, fecha);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> historialPorInscripcion(UUID inscripcionId) {
        return asistenciaRepository.findByInscripcion_InscripcionIdOrderByFechaDesc(inscripcionId);
    }

    /**
     * Corrige una asistencia ya cargada. Se permite aunque la inscripción ya esté dada de baja: corregir un dato
     * histórico mal cargado es válido. La fecha nueva tiene que respetar las mismas reglas que el alta.
     */
    @Transactional
    public Asistencia corregir(UUID asistenciaId, AsistenciaUpdateRequest request) {
        Asistencia existente = asistenciaRepository.findById(asistenciaId)
                .orElseThrow(() -> new RuntimeException("No se encontró la asistencia."));

        if (request.fecha() != null && !request.fecha().equals(existente.getFecha())) {
            validarFecha(existente.getInscripcion(), request.fecha());
            if (asistenciaRepository.existsByInscripcion_InscripcionIdAndFechaAndAsistenciaIdNot(
                    existente.getInscripcion().getInscripcionId(), request.fecha(), asistenciaId)) {
                throw new RuntimeException(mensajeDuplicada(request.fecha()));
            }
            existente.setFecha(request.fecha());
        }
        if (request.presente() != null) {
            existente.setPresente(request.presente());
        }
        if (request.observaciones() != null) {
            existente.setObservaciones(CamposTexto.opcional(request.observaciones()));
        }
        LocalDate fechaFinal = existente.getFecha();
        return guardarTraduciendoDuplicado(() -> asistenciaRepository.saveAndFlush(existente), fechaFinal);
    }

    // ---------- Reglas compartidas por el alta individual, la carga de la clase y la corrección ----------

    // Una asistencia solo puede caer dentro del período en que la inscripción existió, y nunca en el futuro.
    private void validarFecha(Inscripcion inscripcion, LocalDate fecha) {
        if (fecha == null) {
            throw new RuntimeException("La fecha es obligatoria.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new RuntimeException(mensajeFechaFutura(fecha));
        }
        if (fecha.isBefore(inscripcion.getFechaInscripcion())) {
            throw new RuntimeException("La fecha " + fecha.format(FORMATO_FECHA) + " es anterior a la inscripción ("
                    + inscripcion.getFechaInscripcion().format(FORMATO_FECHA) + ").");
        }
        if (inscripcion.getFechaBaja() != null && fecha.isAfter(inscripcion.getFechaBaja())) {
            throw new RuntimeException("La fecha " + fecha.format(FORMATO_FECHA) + " es posterior a la baja de la inscripción ("
                    + inscripcion.getFechaBaja().format(FORMATO_FECHA) + ").");
        }
    }

    // Si la restricción única de la base salta (dos requests simultáneas pasaron el chequeo previo), se responde
    // con el mismo mensaje que el chequeo normal en vez del error técnico de PostgreSQL. Cualquier otra violación
    // de integridad se deja pasar tal cual.
    private <T> T guardarTraduciendoDuplicado(Supplier<T> guardar, LocalDate fecha) {
        try {
            return guardar.get();
        } catch (DataIntegrityViolationException e) {
            String causa = String.valueOf(e.getMostSpecificCause().getMessage());
            if (causa.contains(Asistencia.UK_INSCRIPCION_FECHA)) {
                throw new RuntimeException(mensajeDuplicada(fecha));
            }
            throw e;
        }
    }

    private static String mensajeDuplicada(LocalDate fecha) {
        return "Ya existe un registro de asistencia para este socio en la fecha " + fecha.format(FORMATO_FECHA) + ".";
    }

    private static String mensajeFechaFutura(LocalDate fecha) {
        return "No se puede registrar asistencia en una fecha futura (" + fecha.format(FORMATO_FECHA) + ").";
    }
}
