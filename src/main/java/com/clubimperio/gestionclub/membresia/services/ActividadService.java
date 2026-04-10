package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.entities.Actividad;
import com.clubimperio.gestionclub.membresia.repositories.ActividadRepository;
import com.clubimperio.gestionclub.membresia.repositories.ComisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActividadService {
    private final ActividadRepository actividadRepository;
    private final ComisionRepository comisionRepository;

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
        String nombreNuevo = actividadActualizada.getNombre();

        if (nombreNuevo != null) {
            nombreNuevo = nombreNuevo.trim();
            if (!nombreNuevo.isEmpty()) {
                if (!nombreNuevo.equalsIgnoreCase(actividadExistente.getNombre())) {
                    if (actividadRepository.existsByNombre(nombreNuevo)) {
                        throw new RuntimeException("El nombre '" + nombreNuevo + "' ya está en uso.");
                    }
                }
                actividadExistente.setNombre(nombreNuevo);
            } else {
                throw new RuntimeException("El nombre no puede estar en blanco.");
            }
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
        Actividad actividad = buscarPorId(id);

        if (!actividad.getActivo()) {
            throw new RuntimeException("La actividad ya se encuentra desactivada.");
        }

        actividad.setActivo(false);
        actividadRepository.save(actividad);

        comisionRepository.desactivarComisionesPorActividad(id);

        System.out.println("Actividad " + actividad.getNombre() + " y sus comisiones han sido desactivadas.");
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
