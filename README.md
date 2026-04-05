# 🏰 Club Imperio Juniors - Sistema de Gestión

Sistema integral de gestión para clubes de barrio, desarrollado en Java con Spring Boot. Este software busca digitalizar la administración de socios, actividades, comisiones y cobranzas.

## 🚀 Estado Actual del Proyecto: MVP (Fase 1)
Actualmente el sistema cuenta con el núcleo de gestión de inscripciones operativo y blindado.

### ✨ Características Implementadas
* **Gestión de Personas:** Alta, baja y búsqueda de socios por DNI.
* **Catálogo de Actividades:** Definición de deportes y aranceles mensuales.
* **Control de Comisiones:** Organización de turnos con validación de cupos máximos.
* **Motor de Inscripciones:** Vinculación de socios con actividades, validando disponibilidad de lugar y evitando duplicados.
* **Ciberdefensa de Datos:** Implementación de UUIDs para evitar IDs predecibles y restricciones de integridad en base de datos.

## 🛠️ Stack Tecnológico
* **Lenguaje:** Java 17+
* **Framework:** Spring Boot 3.x
* **Persistencia:** Spring Data JPA + Hibernate
* **Base de Datos:** PostgreSQL
* **Herramientas:** Lombok, Jakarta Persistence, Maven

## 📊 Modelo de Datos (Simplificado)
El flujo de dependencia principal es:
`Actividad` ➡️ `Comisión` ➡️ `Inscripción` ⬅️ `Persona`

> [!IMPORTANT]
> Para conocer las decisiones de diseño y simplificaciones técnicas, consultar el archivo [DEBT.md](./DEBT.md).

## 📂 Documentación de la API
La especificación detallada de los endpoints, parámetros y respuestas se encuentra en:
👉 [api-spec.md](./api-spec.md)

## 🏁 Cómo ejecutar el proyecto
1. Clonar el repositorio.
2. Configurar las credenciales de PostgreSQL en `src/main/resources/application.properties`.
3. Ejecutar `./mvnw spring-boot:run`.
4. El sistema creará automáticamente las tablas (Hibernate create-drop/update).
