# 🏰 Club Imperio Juniors - Sistema de Gestión

Sistema integral de gestión para clubes de barrio, desarrollado en Java con Spring Boot. Este software busca digitalizar la administración de socios, actividades, comisiones, asistencias y cobranzas mediante una arquitectura profesional y escalable.

## 🚀 Estado Actual del Proyecto: MVP (Fase 1.5)
El sistema ha evolucionado a un **Monolito Modular**, con los núcleos de membresía y operativa de campo (asistencias) totalmente funcionales y blindados.

### ✨ Características Implementadas
* **Gestión de Membresía:** Alta, baja y búsqueda de socios por DNI.
* **Catálogo de Actividades:** Definición de deportes y parámetros operativos.
* **Control de Comisiones:** Organización de turnos con validación de cupos máximos y prevención de duplicidad de horarios.
* **Motor de Inscripciones:** Vinculación de socios con actividades, validando disponibilidad de lugar y evitando inscripciones duplicadas.
* **Gestión de Asistencia:** Registro de presente/ausente por clase, historial por socio y validación de "doble carga" en el mismo día.
* **Ciberdefensa de Datos:** Uso de UUIDs (v4) para IDs no predecibles, validaciones de integridad en capa de Servicio y restricciones de unicidad en Base de Datos.

## 📂 Documentación Técnica
Para una comprensión profunda del diseño y uso del sistema, consultá los archivos en la carpeta `/docs`:

* 📜 **[Arquitectura Modular](./docs/ARCHITECTURE.md):** Explicación del diseño por contextos delimitados (Membresía, Asistencia, Pagos).
* 🛠️ **[Deuda Técnica y Decisiones](./docs/DEBT.md):** Registro de compromisos técnicos y simplificaciones de diseño.
* 📡 **[Especificación de la API](./docs/api-spec.md):** Listado detallado de endpoints, parámetros de entrada y modelos de respuesta.

## 🛠️ Stack Tecnológico
* **Lenguaje:** Java 17+
* **Framework:** Spring Boot 3.x
* **Persistencia:** Spring Data JPA + Hibernate
* **Base de Datos:** PostgreSQL
* **Librerías:** Lombok (Boilerplate reduction), Jakarta Persistence (ORM).

## 📊 Modelo de Datos (Flujo de Dominio)
El sistema sigue una jerarquía de dependencia estricta para garantizar la integridad:
`Actividad` ➡️ `Comisión` ➡️ `Inscripción` ⬅️ `Persona` ➡️ `Asistencia` (vía Inscripción)

---

## 🏁 Cómo ejecutar el proyecto
1. Clonar el repositorio.
2. Configurar las credenciales de PostgreSQL en `src/main/resources/application.properties`.
3. Ejecutar `./mvnw spring-boot:run`.
4. El sistema gestionará automáticamente el esquema de tablas mediante Hibernate.