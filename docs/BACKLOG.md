## 🔴 Prioridad 1: Consolidación de Membresía (MVP+)
*Objetivo: Blindar el núcleo de socios para evitar errores humanos y datos huérfanos.*

### 👤 Módulo Persona / Membresía
- **Ticket #1: Refactor de Actualización de Persona**
  - Permitir editar `nombre`, `apellido`, `email`, `telefono`.
  - Permitir edición de `DNI` solo si el nuevo valor no existe en la base de datos (Validación de unicidad).
  - Nota: `esSocio` se elimina de `Persona`; el estado de socio pasa a derivarse de `Membresia` (ver Ticket #2).
- **Ticket #2: Entidad Membresia (reemplaza flag `esSocio`)**
  - Crear entidad `Membresia` (persona_id, fechaAlta, fechaBaja nullable, motivoBaja, auditoría vía JPA Auditing).
  - Endpoint de alta/baja de membresía (la baja admite fecha explícita, incluso futura).
  - Validación: no permitir crear una membresía activa si la persona ya tiene una (`fechaBaja IS NULL`).
  - Índice único parcial en DB: una sola membresía activa por persona.
  - Relación `Persona` → `Membresia`: `cascade = {PERSIST, MERGE}` (sin `orphanRemoval`, para preservar histórico).

### ⚽ Módulo Actividad & Comisión
- **Ticket #3: CRUD Completo de Actividades**
  - Implementar creación, edición de precio y borrado lógico.
  - Restricción: El nombre de la actividad debe ser único.
- **Ticket #4: Validación de Integridad en Comisiones**
  - Impedir la modificación de `horario` si la comisión ya posee alumnos inscriptos (activos o inactivos).
  - Sugerencia: El usuario deberá dar de baja la comisión y crear una nueva para preservar la coherencia histórica.

---

## 🟡 Prioridad 2: Operativa y Finanzas (Fase 2)
*Objetivo: Empezar a gestionar el flujo de dinero y la asistencia avanzada.*

### 📝 Módulo Inscripción
- ~~**Ticket #5: Prevención de Inscripciones Duplicadas**~~ ✅ Resuelto en `InscripcionService`.
- **Ticket #6: Modelo de Cuota Mensual / Revisión de Inscripción**
  - Definir si "cuota mensual" es un concepto calculado o una entidad propia (`Cuota`/`Periodo`).
  - Revisar relación entre `Inscripcion` y la generación de cuotas (¿por actividad, por socio, o ambas?).
  - Prerrequisito antes de avanzar con Ticket #7 (Tesorería).

### 💰 Módulo Tesorería (Pagos)
- **Ticket #7: Entidad Arancel y Registro de Pago**
  - Vincular inscripciones con el precio de la actividad.
  - Generar recibos de pago y control de cuenta corriente del socio.

### 📊 Módulo Reportes
- **Ticket #10: Reportes de Membresía (solo lectura)**
  - `MembresiaController` + `MembresiaService` exclusivamente de lectura/reportes
    (ej: altas del mes, historial de bajas por persona).
  - No contiene lógica de escritura: el alta/baja de membresías sigue
    gestionándose desde el flujo del Ticket #2.
  - Depende de: Ticket #2 estabilizado.

---

## 🟢 Prioridad 3: Calidad y Seguridad
*Objetivo: Profesionalizar la arquitectura interna.*

- **Ticket #8: Implementación de DTOs (Data Transfer Objects)**
  - Separar las entidades JPA de los datos que viajan por la API para evitar "Mass Assignment Vulnerability".
- **Ticket #9: Sistema de Roles (RBAC)**
  - Definir permisos para `ADMIN` (Jefe de administrativos) y `USER` (Administrativo de campo).