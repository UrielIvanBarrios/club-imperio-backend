# Esquema de Base de Datos - Club Imperio Juniors

Motor: **PostgreSQL**. El esquema no se gestiona con migraciones (no hay Flyway/Liquibase); se genera y actualiza automáticamente vía `spring.jpa.hibernate.ddl-auto=update` a partir de las entidades JPA. Todas las PK son `UUID` (v4, `GenerationType.UUID`) para IDs no predecibles.

## Diagrama de relaciones

```
Actividad ──1─────*── Comision ──1─────*── Inscripcion ──*─────1── Persona
                                              │
                                              1
                                              │
                                              *
                                         Asistencia

Persona ──1─────*── ClasePrueba ──*─────1── Actividad

Persona ──1─────*── Membresia
```

## Tabla: `personas`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `persona_id` | UUID | PK, no actualizable |
| `dni` | VARCHAR(20) | `UNIQUE`, `NOT NULL` |
| `nombre` | VARCHAR(100) | `NOT NULL` |
| `apellido` | VARCHAR(100) | `NOT NULL` |
| `fecha_nacimiento` | DATE | nullable |
| `telefono_principal` | VARCHAR(20) | nullable |
| `email` | VARCHAR (default) | nullable |
| `activo` | BOOLEAN | `NOT NULL`, default `true` (soft delete) |
| `fecha_creacion` | TIMESTAMP | seteada por `@PrePersist`, no actualizable |

El estado de socio ya no es una columna en `personas` — se reemplazó por la tabla `membresias` (ver abajo). "¿Es socio activo?" se deriva en código (`Persona.isSocioActivo()`), no se guarda como flag.

## Tabla: `membresias`
Historial de altas/bajas de socio de una persona (reemplaza al viejo flag `es_socio`).

| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `id` | UUID | PK |
| `persona_id` | UUID | FK → `personas.persona_id`, `NOT NULL` |
| `fecha_alta` | TIMESTAMP | `NOT NULL` |
| `fecha_baja` | TIMESTAMP | nullable — `NULL` significa membresía vigente |
| `motivo_baja` | VARCHAR (default) | nullable |

Una persona es "socio activo" si tiene alguna `Membresia` con `fecha_alta <= ahora` y (`fecha_baja IS NULL` o `fecha_baja > ahora`) — permite altas con fecha futura. Sin restricción `UNIQUE`/índice parcial a nivel de BD todavía para impedir dos membresías activas simultáneas en la misma persona (pendiente, ver `docs/BACKLOG.md` Ticket #2).

## Tabla: `actividades`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `actividad_id` | UUID | PK |
| `nombre` | VARCHAR(100) | `UNIQUE`, `NOT NULL` |
| `descripcion` | VARCHAR (default) | nullable |
| `precio_mensual` | DOUBLE | nullable a nivel de columna (validado `NOT NULL` y ≥ 0 en el Service) |
| `activo` | BOOLEAN | `NOT NULL`, default `true` (soft delete) |

## Tabla: `comisiones`
Turno/horario concreto de una actividad.

| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `comision_id` | UUID | PK, no actualizable |
| `actividad_id` | UUID | FK → `actividades.actividad_id`, `NOT NULL` |
| `nombre` | VARCHAR(100) | `NOT NULL` |
| `horario` | VARCHAR(250) | String libre (ej: "Lunes 18hs") — **deuda técnica**, ver `docs/DEBT.md` #1 |
| `cupo_maximo` | INTEGER | nullable (sin tope si es null) |
| `activa` | BOOLEAN | `NOT NULL`, default `true` |

**Restricción compuesta:** `UNIQUE (actividad_id, nombre, horario)` — evita duplicar el mismo turno para una actividad.

## Tabla: `inscripciones`
Vínculo Persona ↔ Comisión (el "contrato" del socio).

| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `inscripcion_id` | UUID | PK, no actualizable |
| `persona_id` | UUID | FK → `personas.persona_id`, `NOT NULL` |
| `comision_id` | UUID | FK → `comisiones.comision_id`, `NOT NULL` |
| `fecha_inscripcion` | DATE | `NOT NULL`, default hoy (`@PrePersist`) |
| `activo` | BOOLEAN | `NOT NULL`, default `true` (soft delete) |
| `fecha_creacion` | TIMESTAMP | seteada por `@PrePersist`, no actualizable |
| `fecha_baja` | DATE | nullable, seteada al dar de baja |

Sin restricción `UNIQUE` a nivel de BD para "una persona no puede tener dos inscripciones activas a la misma comisión" — esa regla se valida en `InscripcionService` (a nivel aplicación, no a nivel esquema).

## Tabla: `clases_prueba`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `clase_prueba_id` | UUID | PK |
| `persona_id` | UUID | FK → `personas.persona_id`, `NOT NULL` |
| `actividad_id` | UUID | FK → `actividades.actividad_id`, `NOT NULL` |
| `fecha_solicitada` | TIMESTAMP | `NOT NULL` |
| `asistio` | BOOLEAN | `NOT NULL`, default `false` |
| `costo` | DOUBLE | nullable (no vinculado aún a `Arancel`, ver `docs/DEBT.md` #3) |
| `observaciones` | VARCHAR(500) | nullable, se usa como historial concatenado |
| `fecha_creacion` | TIMESTAMP | seteada por `@PrePersist`, no actualizable |

## Tabla: `asistencias`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `asistencia_id` | UUID | PK, no actualizable |
| `inscripcion_id` | UUID | FK → `inscripciones.inscripcion_id`, `NOT NULL` |
| `fecha` | DATE | `NOT NULL` |
| `presente` | BOOLEAN | default `true` |
| `observaciones` | VARCHAR(255) | nullable |

Sin restricción `UNIQUE (inscripcion_id, fecha)` a nivel de BD — la prevención de doble carga del mismo día se valida en `AsistenciaService`, no en el esquema.

## Notas generales
* **Fetch por defecto `EAGER`** en todas las relaciones `@ManyToOne` (`Comision.actividad`, `Inscripcion.persona`, `Inscripcion.comision`, `ClasePrueba.persona`, `ClasePrueba.actividad`, `Asistencia.inscripcion`), **excepto `Membresia.persona`, que es `LAZY`**. Esto trae el objeto relacionado completo en cada consulta (salvo esa excepción) — puede impactar performance a medida que crezca el volumen de datos.
* **Soft delete generalizado**: `personas.activo`, `actividades.activo`, `comisiones.activa`, `inscripciones.activo` — ningún módulo borra filas físicamente. `membresias` no tiene columna `activo`: su vigencia se expresa con el rango `fecha_alta`/`fecha_baja` en vez de un flag booleano (igual que `inscripciones`, que combina ambos mecanismos).
* **`Persona → Membresia` es `cascade = {PERSIST, MERGE}` sin `orphanRemoval`**: guardar/actualizar una `Persona` persiste sus `Membresia` nuevas, pero nunca se borra una `Membresia` en cascada (ni por borrado de la `Persona`, ni por sacarla de la lista en memoria) — preserva el historial.
* **Sin auditoría de cambios**: no existen columnas `created_by`/`last_modified_by`, ni tablas de historial de estados (ver `docs/DEBT.md` #2, #5 y #7). `membresias.motivo_baja` es la única columna de "auditoría" de negocio hoy; la auditoría técnica (JPA Auditing) sigue pendiente.
* **Pendiente**: tablas para el futuro módulo de Pagos (`arancel`, `pago`) — no implementadas todavía.