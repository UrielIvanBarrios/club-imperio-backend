package com.clubimperio.gestionclub.services;

import com.clubimperio.gestionclub.entities.Comision;
import com.clubimperio.gestionclub.entities.Inscripcion;
import com.clubimperio.gestionclub.entities.Persona;
import com.clubimperio.gestionclub.repositories.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Comision comision = comisionService.buscarPorId(comisionId);
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
}
