package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.dtos.ComisionUpdateRequest;
import com.clubimperio.gestionclub.membresia.entities.Actividad;
import com.clubimperio.gestionclub.membresia.entities.Comision;
import com.clubimperio.gestionclub.membresia.repositories.ComisionRepository;
import com.clubimperio.gestionclub.membresia.repositories.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComisionService {
    private final ComisionRepository comisionRepository;
    private final ActividadService actividadService;
    // Repository y no InscripcionService: InscripcionService ya depende de ComisionService,
    // y al revés se formaría una dependencia circular. Mismo criterio que ActividadService -> ComisionRepository.
    private final InscripcionRepository inscripcionRepository;

    @Transactional
    public Comision crearComision(UUID actividadId, String nombre, String horario, Integer cupo){
        Actividad actividad = actividadService.buscarPorId(actividadId);
        // Una actividad dada de baja no puede tener comisiones activas (es lo que garantiza su baja en cascada)
        if (!Boolean.TRUE.equals(actividad.getActivo())) {
            throw new RuntimeException("No se puede crear una comisión en una actividad dada de baja.");
        }
        // En el alta, nombre y horario son obligatorios: misma regla y mismos mensajes que en el PUT (ver CamposTexto)
        nombre = CamposTexto.obligatorio(nombre == null ? "" : nombre, "El nombre de la comisión no puede estar vacío.");
        horario = CamposTexto.obligatorio(horario == null ? "" : horario, "El horario de la comisión no puede estar vacío.");
        // null = sin tope de cupo (sigue permitido). Lo que no se permite es 0 o negativo.
        if (cupo != null) {
            validarCupoPositivo(cupo);
        }

        boolean duplicado = comisionRepository.existsByActividad_ActividadIdAndNombreIgnoreCaseAndHorarioIgnoreCase(
                actividadId, nombre, horario
        );

        if (duplicado) {
            throw new RuntimeException("Ya existe una comisión para '" + actividad.getNombre() +
                    "' con el nombre '" + nombre + "' en el horario '" + horario + "'.");
        }

        Comision nueva = Comision.builder()
                .actividad(actividad)
                .nombre(nombre)
                .horario(horario)
                .cupoMaximo(cupo)
                .activa(true)
                .build();

        return comisionRepository.save(nueva);
    }

    @Transactional(readOnly = true)
    public List<Comision> listarPorActividad(UUID actividadId, boolean incluirInactivas){
        return incluirInactivas
                ? comisionRepository.findByActividad_ActividadId(actividadId)
                : comisionRepository.findByActividad_ActividadIdAndActivaTrue(actividadId);
    }

    @Transactional
    public Comision buscarPorId(UUID id){
        return comisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comision no encontrado"));
    }

    // Igual que buscarPorId, pero deja la comisión bloqueada (SELECT ... FOR UPDATE) hasta que termine la transacción
    // del que llama. Propagation REQUIRED (el default): se une a la transacción de la inscripción, así que el bloqueo
    // dura hasta que esa inscripción confirma o se revierte.
    @Transactional
    public Comision buscarPorIdConBloqueo(UUID id) {
        return comisionRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RuntimeException("Comision no encontrado"));
    }

    @Transactional
    public Comision actualizar(UUID id, ComisionUpdateRequest request) {
        // Con bloqueo, por el mismo motivo que la inscripción: si no, se podría achicar el cupo
        // mientras entra una inscripción y quedar con más inscriptos que cupo.
        Comision existente = buscarPorIdConBloqueo(id);
        if (!existente.getActiva()) {
            throw new RuntimeException("No se puede editar una comisión dada de baja.");
        }

        boolean quitarTope = Boolean.TRUE.equals(request.sinTope());
        if (quitarTope && request.cupoMaximo() != null) {
            throw new RuntimeException("No se puede enviar 'cupoMaximo' y 'sinTope': true a la vez. Elegí uno de los dos.");
        }

        String nombreNuevo = existente.getNombre();
        if (request.nombre() != null) {
            nombreNuevo = CamposTexto.obligatorio(request.nombre(), "El nombre de la comisión no puede estar vacío.");
        }
        String horarioNuevo = existente.getHorario();
        if (request.horario() != null) {
            horarioNuevo = CamposTexto.obligatorio(request.horario(), "El horario de la comisión no puede estar vacío.");
        }

        // Misma regla que en el alta (Actividad + Nombre + Horario únicos), excluyendo a esta misma comisión
        boolean duplicado = comisionRepository.existsByActividad_ActividadIdAndNombreIgnoreCaseAndHorarioIgnoreCaseAndComisionIdNot(
                existente.getActividad().getActividadId(), nombreNuevo, horarioNuevo, id
        );
        if (duplicado) {
            throw new RuntimeException("Ya existe otra comisión para '" + existente.getActividad().getNombre() +
                    "' con el nombre '" + nombreNuevo + "' en el horario '" + horarioNuevo + "'.");
        }

        if (request.cupoMaximo() != null) {
            validarCupoPositivo(request.cupoMaximo());
            // No se puede achicar el cupo por debajo de los inscriptos actuales: quedarían "sobrando" alumnos
            long inscriptosActivos = inscripcionRepository.countByComision_ComisionIdAndActivoTrue(id);
            if (request.cupoMaximo() < inscriptosActivos) {
                throw new RuntimeException("El cupo no puede ser menor a los " + inscriptosActivos +
                        " inscriptos activos de la comisión.");
            }
            existente.setCupoMaximo(request.cupoMaximo());
        }
        // Quitar el tope nunca puede dejar inscriptos "sobrando", así que no hace falta validar contra los activos
        if (quitarTope) {
            existente.setCupoMaximo(null);
        }

        existente.setNombre(nombreNuevo);
        existente.setHorario(horarioNuevo);
        return comisionRepository.save(existente);
    }

    // Baja lógica en cascada, mismo criterio que Actividad -> Comisiones: al dar de baja la comisión
    // se dan de baja sus inscripciones activas (con fecha de baja = hoy), así no quedan alumnos
    // "inscriptos" en una comisión que ya no existe. Todo en una transacción: o se hace todo o nada.
    @Transactional
    public void eliminar(UUID id) {
        Comision comision = buscarPorId(id);
        if (!comision.getActiva()) {
            throw new RuntimeException("La comisión ya se encuentra dada de baja.");
        }
        comision.setActiva(false);
        comisionRepository.save(comision);

        int bajas = inscripcionRepository.darDeBajaActivasPorComision(id, LocalDate.now());
        log.info("Comisión '{}' dada de baja junto con {} inscripciones activas.", comision.getNombre(), bajas);
    }

    // Compartida por alta (POST) y edición (PUT) para que la regla sea la misma en los dos.
    // Un cupo de 0 dejaría una comisión activa en la que nadie se puede inscribir.
    private void validarCupoPositivo(Integer cupo) {
        if (cupo <= 0) {
            throw new RuntimeException("El cupo máximo debe ser mayor a cero.");
        }
    }

}
