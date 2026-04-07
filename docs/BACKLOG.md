# 📋 Backlog de Desarrollo - Club Imperio Juniors

Este documento lista las funcionalidades, reglas de negocio y mejoras planificadas para el sistema, organizadas por prioridad y módulos.

---

## 🔴 Prioridad 1: Consolidación de Membresía (MVP+)
*Objetivo: Blindar el núcleo de socios para evitar errores humanos y datos huérfanos.*

### 👤 Módulo Persona
- **Ticket #1: Refactor de Actualización de Persona**
    - Permitir editar `nombre`, `apellido`, `email`, `telefono` y `esSocio`.
    - Permitir edición de `DNI` solo si el nuevo valor no existe en la base de datos (Validación de unicidad).
- **Ticket #2: Gestión de Vínculo Social**
    - Crear endpoint para "Dar de Alta/Baja como Socio" (cambiar el flag `esSocio`) sin afectar la existencia de la Persona en el sistema.

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
- **Ticket #5: Prevención de Inscripciones Duplicadas**
    - Validar que una persona no pueda inscribirse dos veces a la misma comisión activa.
- **Ticket #6: Motor de Bajas Programadas (V2)**
    - Implementar `fechaBajaEfectiva`.
    - Desarrollar lógica para que el cupo se libere recién cuando se cumpla la fecha de baja, permitiendo al socio asistir hasta el fin del periodo abonado.

### 💰 Módulo Tesorería (Pagos)
- **Ticket #7: Entidad Arancel y Registro de Pago**
    - Vincular inscripciones con el precio de la actividad.
    - Generar recibos de pago y control de cuenta corriente del socio.

---

## 🟢 Prioridad 3: Calidad y Seguridad
*Objetivo: Profesionalizar la arquitectura interna.*

- **Ticket #8: Implementación de DTOs (Data Transfer Objects)**
    - Separar las entidades JPA de los datos que viajan por la API para evitar "Mass Assignment Vulnerability".
- **Ticket #9: Sistema de Roles (RBAC)**
    - Definir permisos para `ADMIN` (Jefe de administrativos) y `USER` (Administrativo de campo).