package com.clubimperio.gestionclub.repositories;

import com.clubimperio.gestionclub.entities.Comision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComisionRepository extends JpaRepository<Comision, UUID> {

    List<Comision> findByActividad_ActividadId(UUID actividadId);

    List<Comision> findByActivaTrue();
}
