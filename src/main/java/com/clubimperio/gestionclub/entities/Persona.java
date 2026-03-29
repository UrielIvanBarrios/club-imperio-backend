package com.clubimperio.gestionclub.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "persona_id", updatable = false, nullable = false)
    private UUID personaId;

    @Column(unique = true, nullable = false, length = 20)
    private String dni;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono_principal", length = 20)
    private String telefonoPrincipal;

    private String email;

    @Column(name = "es_socio")
    private Boolean esSocio = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    public void setActivo(Boolean activo) {
        this.activo = (activo == null) ? true : activo;
    }
}
