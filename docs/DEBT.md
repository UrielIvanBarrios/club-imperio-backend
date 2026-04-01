# Registro de Deuda Técnica - Club Imperio

Este documento detalla las simplificaciones tomadas para el MVP y las mejoras necesarias a futuro.

## 1. Módulo Comisiones: Horarios como String
* **Estado actual:** El campo `horario` es un `String` libre (ej: "Lunes 18hs").
* **Riesgo:** Inconsistencia de datos, errores de tipeo y dificultad para validar solapamientos de profesores o salones.
* **Mejora necesaria:** Crear una entidad `Horario` con `DiaSemana` (Enum), `HoraInicio` y `HoraFin`.

## 2. Módulo Personas: Auditoría de Bajas
* **Estado actual:** Usamos un booleano `activo`.
* **Riesgo:** Si se da de baja a alguien, no sabemos el motivo ni quién lo hizo.
* **Mejora necesaria:** Implementar una tabla de logs o campos de auditoría `motivo_baja` y `fecha_baja` real.

## 3. Clases de Prueba: Integración con Pagos
* **Estado actual:** Tienen un campo `costo` manual.
* **Mejora necesaria:** Vincular la Clase de Prueba con un `Arancel` específico para que pase por la caja oficial del club.

## 4. Capa de Seguridad: Implementación de DTOs
* **Estado actual:** El Controller recibe y devuelve Entidades directas (@Entity).
* **Riesgo:** "Mass Assignment Vulnerability". Un usuario malintencionado podría enviar un JSON con `"activo": true` o `"esSocio": true` en un endpoint de actualización y el sistema lo guardaría sin validar.
* **Mejora:** Crear `PersonaDTO`, `InscripcionRequest`, etc., para filtrar qué datos entran y salen del sistema.

## 5. Auditoría Blindada: Historial de Estados
* **Estado actual:** Booleano `activo` simple.
* **Riesgo:** Pérdida de trazabilidad. No sabemos por qué un socio dejó el club (Mora vs. Baja Voluntaria).
* **Mejora:** Tabla `historial_estados_persona` para registrar: Fecha, Estado Anterior, Estado Nuevo y Motivo.

## 6. Transición de Datos: Migrador de Excel
* **Estado actual:** Carga manual de datos.
* **Necesidad:** El club tiene miles de registros en Excel. La carga manual es propensa a errores.
* **Mejora:** Desarrollar un servicio de importación masiva con validación de integridad (limpieza de DNI duplicados, formatos de fecha, etc.).

## 7. Trazabilidad de Usuario (Spring Security)
* **Estado actual:** No hay sistema de login.
* **Mejora:** Implementar `@CreatedBy` y `@LastModifiedBy`. En el club es vital saber qué administrativo borró una clase o cobró un arancel por seguridad contable.