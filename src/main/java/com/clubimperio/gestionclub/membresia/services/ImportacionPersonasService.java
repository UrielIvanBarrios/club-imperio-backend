package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.dtos.ImportacionResultado;
import com.clubimperio.gestionclub.membresia.dtos.ImportacionResultado.FilaConError;
import com.clubimperio.gestionclub.membresia.dtos.ImportacionResultado.FilaValida;
import com.clubimperio.gestionclub.membresia.entities.Persona;
import com.clubimperio.gestionclub.membresia.repositories.PersonaRepository;
import com.clubimperio.gestionclub.membresia.services.LectorArchivoTabular.ContenidoArchivo;
import com.clubimperio.gestionclub.membresia.services.LectorArchivoTabular.FilaArchivo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Importación masiva de socios desde CSV/Excel (DEBT #6).
 * Política: se validan TODAS las filas primero y se informa cada error con su número de fila.
 * Las filas válidas se guardan juntas en una sola transacción; las que tienen errores se saltean.
 * Con simular=true solo se valida (para que el front muestre una vista previa antes de confirmar).
 */
@Slf4j
@Service
public class ImportacionPersonasService {

    // Columnas reconocidas (clave = nombre canónico). Se aceptan varios nombres por columna,
    // comparados sin tildes, mayúsculas ni espacios: "Fecha de Nacimiento" == "fecha_nacimiento".
    private static final Map<String, List<String>> ALIAS = Map.of(
            "dni", List.of("dni", "documento", "nrodocumento", "nrodni"),
            "nombre", List.of("nombre", "nombres"),
            "apellido", List.of("apellido", "apellidos"),
            "fechaNacimiento", List.of("fechanacimiento", "fechadenacimiento", "nacimiento"),
            "telefono", List.of("telefono", "telefonoprincipal", "celular", "tel"),
            "email", List.of("email", "mail", "correo", "correoelectronico"),
            "socio", List.of("socio", "essocio", "seasocia")
    );
    private static final List<String> OBLIGATORIAS = List.of("dni", "nombre", "apellido");

    // La regla del DNI no está acá: vive en PersonaService (normalizarDni / esDniValido), compartida con el alta manual
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    // STRICT + "uuuu" rechaza fechas imposibles como 31/02/2000 en vez de "corregirlas" al 2/3
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT)
    );
    private static final Set<String> SI = Set.of("si", "sí", "s", "true", "1", "x");
    private static final Set<String> NO = Set.of("", "no", "n", "false", "0");

    private final LectorArchivoTabular lector;
    private final PersonaService personaService;
    private final PersonaRepository personaRepository;
    private final int maxFilas;

    public ImportacionPersonasService(LectorArchivoTabular lector,
                                      PersonaService personaService,
                                      PersonaRepository personaRepository,
                                      @Value("${app.importacion.max-filas:5000}") int maxFilas) {
        this.lector = lector;
        this.personaService = personaService;
        this.personaRepository = personaRepository;
        this.maxFilas = maxFilas;
    }

    // Todo en una transacción: si falla el guardado de cualquier fila válida (por ejemplo, otro usuario cargó
    // el mismo DNI mientras tanto), se deshace la importación entera y no queda una carga "a medias".
    @Transactional
    public ImportacionResultado importar(MultipartFile archivo, boolean simular) {
        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("No se recibió ningún archivo (campo 'archivo').");
        }

        ContenidoArchivo contenido = lector.leer(archivo);
        Map<String, String> columnas = mapearColumnas(contenido.encabezados());

        List<FilaArchivo> filas = contenido.filas().stream().filter(f -> !f.estaVacia()).toList();
        if (filas.isEmpty()) {
            throw new RuntimeException("El archivo no tiene filas con datos debajo del encabezado.");
        }
        if (filas.size() > maxFilas) {
            throw new RuntimeException("El archivo tiene " + filas.size() + " filas; el máximo por importación es "
                    + maxFilas + ". Dividilo en varios archivos.");
        }

        Set<String> dnisEnBase = buscarDnisExistentes(filas, columnas);
        Map<String, Integer> primeraFilaPorDni = new HashMap<>();

        List<FilaValida> validas = new ArrayList<>();
        List<FilaConError> errores = new ArrayList<>();
        List<Persona> aGuardar = new ArrayList<>();
        List<Boolean> seAsocian = new ArrayList<>();

        for (FilaArchivo fila : filas) {
            List<String> erroresFila = new ArrayList<>();

            // --- DNI ---
            String dniOriginal = valor(fila, columnas, "dni");
            String dni = PersonaService.normalizarDni(dniOriginal);
            if (dni.isEmpty()) {
                erroresFila.add("El DNI es obligatorio.");
            } else if (!PersonaService.esDniValido(dni)) {
                erroresFila.add(PersonaService.mensajeDniInvalido(dniOriginal));
            } else if (primeraFilaPorDni.containsKey(dni)) {
                erroresFila.add("DNI repetido en el archivo (ya aparece en la fila " + primeraFilaPorDni.get(dni) + ").");
            } else {
                primeraFilaPorDni.put(dni, fila.numero());
                if (dnisEnBase.contains(dni)) {
                    erroresFila.add("DNI duplicado: ya existe una persona registrada con el DNI " + dni + ".");
                }
            }

            // --- Nombre y apellido ---
            String nombre = valor(fila, columnas, "nombre");
            String apellido = valor(fila, columnas, "apellido");
            validarTextoObligatorio(nombre, "El nombre", 100, erroresFila);
            validarTextoObligatorio(apellido, "El apellido", 100, erroresFila);

            // --- Opcionales ---
            LocalDate fechaNacimiento = parsearFecha(valor(fila, columnas, "fechaNacimiento"), erroresFila);

            String telefono = valor(fila, columnas, "telefono");
            if (telefono.length() > 20) {
                erroresFila.add("El teléfono no puede superar los 20 caracteres.");
            }

            String email = valor(fila, columnas, "email").toLowerCase();
            if (!email.isEmpty() && (email.length() > 255 || !EMAIL.matcher(email).matches())) {
                erroresFila.add("Email con formato inválido: '" + email + "'.");
            }

            String socioTexto = valor(fila, columnas, "socio").toLowerCase();
            boolean socio = SI.contains(socioTexto);
            if (!socio && !NO.contains(socioTexto)) {
                erroresFila.add("Valor inválido en 'socio': '" + socioTexto + "' (usar SI o NO).");
            }

            if (!erroresFila.isEmpty()) {
                errores.add(new FilaConError(fila.numero(), dniOriginal, erroresFila));
                continue;
            }

            validas.add(new FilaValida(fila.numero(), dni, nombre, apellido, socio));
            aGuardar.add(Persona.builder()
                    .dni(dni)
                    .nombre(nombre)
                    .apellido(apellido)
                    .fechaNacimiento(fechaNacimiento)
                    .telefonoPrincipal(telefono.isEmpty() ? null : telefono)
                    .email(email.isEmpty() ? null : email)
                    .activo(true) // Persona no usa @Builder.Default en "activo": hay que setearlo explícito
                    .build());
            seAsocian.add(socio);
        }

        int importadas = 0;
        if (!simular) {
            // Se reutiliza el mismo alta que el ABM manual (PersonaService.guardar): misma validación de DNI
            // y misma creación de la primera Membresia. La regla vive en un solo lugar.
            for (int i = 0; i < aGuardar.size(); i++) {
                personaService.guardar(aGuardar.get(i), seAsocian.get(i), null);
            }
            importadas = aGuardar.size();
            log.info("Importación '{}': {} personas importadas, {} filas con error.",
                    archivo.getOriginalFilename(), importadas, errores.size());
        }

        return new ImportacionResultado(archivo.getOriginalFilename(), simular, filas.size(),
                validas.size(), errores.size(), importadas, validas, errores);
    }

    /** CSV de ejemplo para descargar: separador ";" y BOM UTF-8, así Excel en español lo abre bien y con tildes. */
    public byte[] plantillaCsv() {
        String contenido = "﻿"
                + "dni;nombre;apellido;fecha_nacimiento;telefono;email;socio\n"
                + "30123456;María;Gómez;15/03/1990;1155551234;maria.gomez@mail.com;SI\n"
                + "35987654;Juan;Pérez;02/11/2010;;;NO\n";
        return contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---------- helpers ----------

    // Devuelve columna canónica -> encabezado normalizado presente en el archivo
    private Map<String, String> mapearColumnas(List<String> encabezados) {
        Set<String> presentes = new HashSet<>();
        for (String e : encabezados) {
            String normalizado = LectorArchivoTabular.normalizarEncabezado(e);
            if (!normalizado.isEmpty() && !presentes.add(normalizado)) {
                throw new RuntimeException("La columna '" + e + "' está repetida en el encabezado.");
            }
        }

        Map<String, String> columnas = new HashMap<>();
        ALIAS.forEach((canonica, alias) -> alias.stream()
                .filter(presentes::contains)
                .findFirst()
                .ifPresent(encontrada -> columnas.put(canonica, encontrada)));

        List<String> faltantes = OBLIGATORIAS.stream().filter(c -> !columnas.containsKey(c)).toList();
        if (!faltantes.isEmpty()) {
            throw new RuntimeException("Faltan columnas obligatorias en el encabezado: " + String.join(", ", faltantes)
                    + ". Columnas esperadas: dni, nombre, apellido, fecha_nacimiento, telefono, email, socio.");
        }
        return columnas;
    }

    private String valor(FilaArchivo fila, Map<String, String> columnas, String canonica) {
        String columna = columnas.get(canonica);
        return columna == null ? "" : fila.get(columna);
    }

    // Una sola consulta (por tandas de 1000) en vez de una por fila: con miles de filas la diferencia es grande
    private Set<String> buscarDnisExistentes(List<FilaArchivo> filas, Map<String, String> columnas) {
        List<String> dnis = filas.stream()
                .map(f -> PersonaService.normalizarDni(valor(f, columnas, "dni")))
                .filter(PersonaService::esDniValido)
                .distinct()
                .toList();
        Set<String> existentes = new HashSet<>();
        for (int i = 0; i < dnis.size(); i += 1000) {
            existentes.addAll(personaRepository.findDnisExistentes(dnis.subList(i, Math.min(i + 1000, dnis.size()))));
        }
        return existentes;
    }

    private void validarTextoObligatorio(String valor, String campo, int max, List<String> errores) {
        if (valor.isEmpty()) {
            errores.add(campo + " es obligatorio.");
        } else if (valor.length() > max) {
            errores.add(campo + " no puede superar los " + max + " caracteres.");
        }
    }

    private LocalDate parsearFecha(String texto, List<String> errores) {
        if (texto.isEmpty()) return null;
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                LocalDate fecha = LocalDate.parse(texto, formato);
                if (fecha.isAfter(LocalDate.now())) {
                    errores.add("La fecha de nacimiento no puede ser futura: '" + texto + "'.");
                    return null;
                }
                return fecha;
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        errores.add("Fecha de nacimiento inválida: '" + texto + "' (usar dd/mm/aaaa).");
        return null;
    }
}
