> **Estado:** ✅ hecho · 🟠 parcial · ⬜ pendiente. La deuda técnica detectada durante el desarrollo está en [`DEBT.md`](./DEBT.md).

## 🔴 Prioridad 1: Consolidación de Membresía (MVP+)
*Objetivo: Blindar el núcleo de socios para evitar errores humanos y datos huérfanos.*

### 👤 Módulo Persona / Membresía
- 🟠 **Ticket #1: Refactor de Actualización de Persona**
  - ✅ Permitir editar `nombre`, `apellido`, `email`, `telefono` (`PersonaUpdateRequest`).
  - ⬜ Permitir edición de `DNI` solo si el nuevo valor no existe en la base de datos (Validación de unicidad). Hoy el DNI no es editable.
  - ✅ `esSocio` se eliminó de `Persona`: el estado de socio se deriva de `Membresia` (ver Ticket #2).
  - ✅ *(agregado)* El DNI se normaliza y se valida su formato (6 a 8 dígitos) en el alta. Falta normalizarlo en las búsquedas (ver `DEBT.md` #9).
- 🟠 **Ticket #2: Entidad Membresia (reemplaza flag `esSocio`)**
  - ✅ Crear entidad `Membresia` (persona_id, fechaAlta, fechaBaja nullable, motivoBaja). ⬜ Auditoría vía JPA Auditing.
  - ⬜ Endpoint de alta/baja de membresía (la baja admite fecha explícita, incluso futura). Hoy la primera membresía se crea en el alta de la persona o en la importación (`seAsocia` / columna `socio`).
  - ⬜ Validación: no permitir crear una membresía activa si la persona ya tiene una (`fechaBaja IS NULL`).
  - ⬜ Índice único parcial en DB: una sola membresía activa por persona. Bloqueado por la falta de migraciones (ver `DEBT.md` #10).
  - ✅ Relación `Persona` → `Membresia`: `cascade = {PERSIST, MERGE}` (sin `orphanRemoval`, para preservar histórico).

### ⚽ Módulo Actividad & Comisión
- ✅ **Ticket #3: CRUD Completo de Actividades**
  - ✅ Implementar creación, edición de precio y borrado lógico.
  - ✅ Restricción: El nombre de la actividad debe ser único.
  - ✅ *(agregado)* La baja desactiva en cascada sus comisiones **y las inscripciones** de esas comisiones.
- ⬜ **Ticket #4: Validación de Integridad en Comisiones**
  - ⬜ Impedir la modificación de `horario` si la comisión ya posee alumnos inscriptos (activos o inactivos).
  - Sugerencia: El usuario deberá dar de baja la comisión y crear una nueva para preservar la coherencia histórica.
  - ✅ *(prerrequisito, hecho)* `PUT /api/comisiones/{id}` (nombre, horario, cupo) y `DELETE` con baja lógica en cascada sobre las inscripciones. El cupo no puede ser 0 ni menor a los inscriptos activos.

---

## 🟡 Prioridad 2: Operativa y Finanzas (Fase 2)
*Objetivo: Empezar a gestionar el flujo de dinero y la asistencia avanzada.*

### 📝 Módulo Inscripción
- ✅ ~~**Ticket #5: Prevención de Inscripciones Duplicadas**~~ Resuelto en `InscripcionService`.
- ⬜ **Ticket #6: Modelo de Cuota Mensual / Revisión de Inscripción**
  - Definir si "cuota mensual" es un concepto calculado o una entidad propia (`Cuota`/`Periodo`).
  - Revisar relación entre `Inscripcion` y la generación de cuotas (¿por actividad, por socio, o ambas?).
  - Prerrequisito antes de avanzar con Ticket #7 (Tesorería).

### 💰 Módulo Tesorería (Pagos)
- ⬜ **Ticket #7: Entidad Arancel y Registro de Pago**
  - Vincular inscripciones con el precio de la actividad.
  - Generar recibos de pago y control de cuenta corriente del socio.
  - Nota: en la base ya existen tablas de un diseño previo (`aranceles`, `transacciones`, `detalle_pagos`, `vinculos_familiares`) sin entidades JPA. `transacciones` referencia a `usuarios.usuario_id` (UUID), compatible con la entidad `Usuario` actual.

### 📊 Módulo Reportes
- ⬜ **Ticket #10: Reportes de Membresía (solo lectura)**
  - `MembresiaController` + `MembresiaService` exclusivamente de lectura/reportes
    (ej: altas del mes, historial de bajas por persona).
  - No contiene lógica de escritura: el alta/baja de membresías sigue
    gestionándose desde el flujo del Ticket #2.
  - Depende de: Ticket #2 estabilizado.

### 📥 Módulo Importación
- ✅ **Ticket #11: Importación masiva de socios** *(agregado, resuelve `DEBT.md` #6)*
  - `POST /api/personas/importar` (solo ADMIN): CSV o `.xlsx`, con un reporte de filas válidas y de filas con error, y modo vista previa (`?simular=true`).
  - `GET /api/personas/importar/plantilla`: plantilla CSV descargable.

---

## 🟢 Prioridad 3: Calidad y Seguridad
*Objetivo: Profesionalizar la arquitectura interna.*

- 🟠 **Ticket #8: Implementación de DTOs (Data Transfer Objects)**
  - Separar las entidades JPA de los datos que viajan por la API para evitar "Mass Assignment Vulnerability".
  - ✅ Personas, Usuarios y el PUT de Comisiones. ⬜ Actividades, el resto de Comisiones, Inscripciones, Asistencias y Clases de Prueba (ver `DEBT.md` #4).
- ✅ **Ticket #9: Sistema de Roles (RBAC)**
  - ✅ Definir permisos para `ADMIN` (Jefe de administrativos) y `USER` (Administrativo de campo).
  - ✅ Spring Security + JWT: `POST /api/auth/login`, `GET /api/auth/me`, `PUT /api/auth/me/password`.
  - ✅ ABM de usuarios (`/api/usuarios`) e importación masiva solo para `ADMIN`; el resto de la API, para cualquier usuario autenticado.
  - ⬜ Pendientes de seguridad (revocación de tokens, límite de intentos de login): ver `DEBT.md` #11.
- ⬜ **Ticket #12: Tests automatizados** *(agregado)*
  - Hoy solo existe `contextLoads()`. Plan de cobertura mínima en `DEBT.md` #13.
