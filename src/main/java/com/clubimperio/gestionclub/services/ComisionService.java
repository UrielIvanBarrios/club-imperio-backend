package com.clubimperio.gestionclub.services;

import com.clubimperio.gestionclub.entities.Actividad;
import com.clubimperio.gestionclub.entities.Comision;
import com.clubimperio.gestionclub.repositories.ComisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComisionService {
    private final ComisionRepository comisionRepository;
    private final ActividadService actividadService;

    @Transactional
    public Comision crearComision(UUID actividadId, String nombre, String horario, Integer cupo){
        Actividad actividad = actividadService.buscarPorId(actividadId);

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
    public List<Comision> listarPorActividad(UUID actividadId){
        return comisionRepository.findByActividad_ActividadId(actividadId);
    }

    @Transactional
    public Comision buscarPorId(UUID id){
        return comisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comision no encontrado"));
    }

}
