package com.clubimperio.gestionclub.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "comisiones")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comision_id", updatable = false, nullable = false)
    private UUID comisionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(nullable = false, length = 100)
    private String nombre;

    // TODO: [DEUDA TÉCNICA] Refactorizar horario de String a Entidad para evitar solapamientos.
    @Column(length = 250)
    private String horario;

    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;


    public void setActiva(Boolean activa) {
        this.activa = activa;
    }
}
