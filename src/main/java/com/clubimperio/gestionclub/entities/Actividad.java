package com.clubimperio.gestionclub.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;


@Entity
@Table(name = "actividades")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "actividad_id", columnDefinition = "UUID")
    private UUID actividadId;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    private String descripcion;


    private Double precioMensual;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    public void setActivo(Boolean activo) {
        this.activo = (activo == null) ? true : activo;
    }

}
