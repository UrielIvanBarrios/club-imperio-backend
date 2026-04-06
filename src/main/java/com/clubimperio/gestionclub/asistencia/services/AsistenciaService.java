package com.clubimperio.gestionclub.asistencia.services;

import com.clubimperio.gestionclub.asistencia.entities.Asistencia;
import com.clubimperio.gestionclub.membresia.entities.Inscripcion;
import com.clubimperio.gestionclub.membresia.services.InscripcionService;
import com.clubimperio.gestionclub.asistencia.repositories.AsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsistenciaService {
    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionService inscripcionService;

    @Transactional
    public Asistencia registrarAsistencia(UUID inscripcionId, LocalDate fecha, Boolean presente, String observaciones) {

        Inscripcion inscripcion = inscripcionService.buscarPorId(inscripcionId);

        if (inscripcion.getActivo() == null || !inscripcion.getActivo()) {
            throw new RuntimeException("No se puede registrar asistencia de una inscripción dada de baja.");
        }

        if (asistenciaRepository.existsByInscripcion_InscripcionIdAndFecha(inscripcionId, fecha)) {
            throw new RuntimeException("Ya existe un registro de asistencia para este socio en la fecha: " + fecha);
        }

        Asistencia nuevaAsistencia = Asistencia.builder()
                .inscripcion(inscripcion)
                .fecha(fecha)
                .presente(presente)
                .observaciones(observaciones)
                .build();

        return asistenciaRepository.save(nuevaAsistencia);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> historialPorInscripcion(UUID inscripcionId) {
        return asistenciaRepository.findByInscripcion_InscripcionIdOrderByFechaDesc(inscripcionId);
    }
}
