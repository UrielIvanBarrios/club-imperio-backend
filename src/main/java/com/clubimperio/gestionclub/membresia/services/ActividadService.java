package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.entities.Actividad;
import com.clubimperio.gestionclub.membresia.repositories.ActividadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActividadService {
    private final ActividadRepository actividadRepository;

    @Transactional(readOnly = true)
    public List<Actividad> listarTodas() {
        return actividadRepository.findAll();
    }

    @Transactional
    public Actividad guardar(Actividad actividad) {
        validarDatosActividad(actividad);
        String nombreNormalizado = actividad.getNombre().trim();
        if (actividadRepository.existsByNombre(nombreNormalizado)) {
            throw new RuntimeException("Ya existe una actividad registrada con el nombre: " + nombreNormalizado);
        }
        if (actividad.getActivo() == null) {
            actividad.setActivo(true);
        }
        actividad.setNombre(nombreNormalizado);
        return actividadRepository.save(actividad);
    }

    @Transactional(readOnly = true)
    public Actividad buscarPorId(UUID id) {
        return actividadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actividad no encontrada con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Actividad buscarPorNombre(String nombre) {
        return actividadRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("No se encontró la actividad: " + nombre));
    }

    @Transactional
    public Actividad actualizar(UUID id, Actividad actividadActualizada) {
        Actividad actividadExistente = buscarPorId(id);

        validarDatosParaUpdate(actividadActualizada);

        // Validar si quiere cambiar el nombre a uno que ya existe en OTRA actividad
        if (actividadActualizada.getNombre() != null &&
                !actividadExistente.getNombre().equalsIgnoreCase(actividadActualizada.getNombre())) {
            if (actividadRepository.existsByNombre(actividadActualizada.getNombre())) {
                throw new RuntimeException("No se puede actualizar: El nombre " + actividadActualizada.getNombre() + " ya está en uso.");
            }
        }

        // Pisamos los datos
        if (actividadActualizada.getNombre() != null) {
            actividadExistente.setNombre(actividadActualizada.getNombre());
        }
        if (actividadActualizada.getDescripcion() != null) {
            actividadExistente.setDescripcion(actividadActualizada.getDescripcion());
        }

        if (actividadActualizada.getPrecioMensual() != null) {
            actividadExistente.setPrecioMensual(actividadActualizada.getPrecioMensual());
        }

        if (actividadActualizada.getActivo() != null) {
            actividadExistente.setActivo(actividadActualizada.getActivo());
        }

        return actividadRepository.save(actividadExistente);
    }

    @Transactional
    public void eliminar(UUID id) {
        Actividad actividad = buscarPorId(id); // Ya incluye el chequeo de existencia

        if (!actividad.getActivo()) {
            throw new RuntimeException("La actividad ya se encuentra desactivada.");
        }

        actividad.setActivo(false);
        actividadRepository.save(actividad);
    }

    private void validarDatosActividad(Actividad a) {
        if (a.getNombre() == null || a.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la actividad no puede estar vacío.");
        }
        if (a.getPrecioMensual() == null || a.getPrecioMensual() < 0) {
            throw new RuntimeException("El precio mensual no puede ser nulo ni negativo.");
        }
    }

    private void validarDatosParaUpdate(Actividad a) {
        if (a.getPrecioMensual() != null && a.getPrecioMensual() < 0) {
            throw new RuntimeException("El precio no puede ser negativo.");
        }
    }

}
