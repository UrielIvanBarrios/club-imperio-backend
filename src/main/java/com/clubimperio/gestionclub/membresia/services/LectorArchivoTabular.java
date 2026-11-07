package com.clubimperio.gestionclub.membresia.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/**
 * Convierte un .csv o .xlsx en filas genéricas (encabezado normalizado -> valor como texto).
 * No sabe nada de socios: solo LEE. Validar es trabajo de ImportacionPersonasService.
 */
@Component
public class LectorArchivoTabular {

    // Fila del archivo: número tal como se ve en Excel (1 = encabezado) y valores por columna normalizada
    public record FilaArchivo(int numero, Map<String, String> valores) {
        public String get(String columna) {
            String v = valores.get(columna);
            return v == null ? "" : v.trim();
        }
        public boolean estaVacia() {
            return valores.values().stream().allMatch(v -> v == null || v.isBlank());
        }
    }

    public record ContenidoArchivo(List<String> encabezados, List<FilaArchivo> filas) {}

    public ContenidoArchivo leer(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        try {
            if (nombre.endsWith(".csv")) {
                return leerCsv(archivo.getBytes());
            }
            if (nombre.endsWith(".xlsx")) {
                try (InputStream in = archivo.getInputStream()) {
                    return leerXlsx(in);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo. Verificá que no esté dañado.");
        }
        throw new RuntimeException("Formato no soportado. Subí un archivo .csv o .xlsx.");
    }

    // Encabezados comparables: "Fecha de Nacimiento" -> "fechadenacimiento", "Teléfono" -> "telefono"
    public static String normalizarEncabezado(String encabezado) {
        if (encabezado == null) return "";
        String sinTildes = Normalizer.normalize(encabezado, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    // ---------- CSV ----------

    private ContenidoArchivo leerCsv(byte[] bytes) throws IOException {
        String texto = decodificar(bytes);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1); // BOM que agrega Excel al "Guardar como CSV UTF-8"
        }

        // Excel en español exporta CSV con ";" y en inglés con ",": se detecta mirando el encabezado
        String primeraLinea = texto.lines().findFirst().orElse("");
        char separador = contar(primeraLinea, ';') > contar(primeraLinea, ',') ? ';' : ',';

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(separador)
                .setIgnoreEmptyLines(false) // así el número de registro coincide con el número de línea
                .get();

        List<String> encabezados = null;
        List<FilaArchivo> filas = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(texto), formato)) {
            for (CSVRecord registro : parser) {
                if (encabezados == null) {
                    encabezados = new ArrayList<>();
                    registro.forEach(encabezados::add);
                    continue;
                }
                Map<String, String> valores = new LinkedHashMap<>();
                for (int i = 0; i < encabezados.size(); i++) {
                    valores.put(normalizarEncabezado(encabezados.get(i)), i < registro.size() ? registro.get(i) : "");
                }
                filas.add(new FilaArchivo((int) registro.getRecordNumber(), valores));
            }
        } catch (UncheckedIOException | IllegalStateException e) {
            throw new RuntimeException("El CSV tiene un formato inválido (revisá las comillas).");
        }
        return new ContenidoArchivo(encabezados == null ? List.of() : encabezados, filas);
    }

    // UTF-8 si es válido; si no, Windows-1252 (lo que usa Excel en Windows al guardar "CSV (delimitado por comas)").
    // Sin esto, los nombres con tildes o ñ llegarían rotos ("Pe�a").
    private String decodificar(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    private static long contar(String texto, char c) {
        return texto.chars().filter(ch -> ch == c).count();
    }

    // ---------- Excel (.xlsx) ----------

    private ContenidoArchivo leerXlsx(InputStream in) throws IOException {
        try (Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0); // se usa la primera hoja
            DataFormatter formateador = new DataFormatter();
            FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();

            Row filaEncabezado = hoja.getRow(hoja.getFirstRowNum());
            if (filaEncabezado == null) {
                return new ContenidoArchivo(List.of(), List.of());
            }
            List<String> encabezados = new ArrayList<>();
            for (int c = 0; c < filaEncabezado.getLastCellNum(); c++) {
                encabezados.add(textoDeCelda(filaEncabezado.getCell(c), formateador, evaluador));
            }

            List<FilaArchivo> filas = new ArrayList<>();
            for (int r = filaEncabezado.getRowNum() + 1; r <= hoja.getLastRowNum(); r++) {
                Row fila = hoja.getRow(r);
                Map<String, String> valores = new LinkedHashMap<>();
                for (int c = 0; c < encabezados.size(); c++) {
                    Cell celda = fila == null ? null : fila.getCell(c);
                    valores.put(normalizarEncabezado(encabezados.get(c)), textoDeCelda(celda, formateador, evaluador));
                }
                filas.add(new FilaArchivo(r + 1, valores)); // r es base 0; Excel muestra base 1
            }
            return new ContenidoArchivo(encabezados, filas);
        } catch (org.apache.poi.EmptyFileException | org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException e) {
            throw new RuntimeException("El archivo .xlsx está vacío o no es un Excel válido.");
        }
    }

    private String textoDeCelda(Cell celda, DataFormatter formateador, FormulaEvaluator evaluador) {
        if (celda == null) return "";
        // Las fechas en Excel son números con formato de fecha: se pasan a ISO (aaaa-mm-dd) para no depender
        // del formato regional de la planilla (dd/mm vs mm/dd).
        CellType tipo = celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
        if (tipo == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            return celda.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        // DataFormatter devuelve el texto tal como se ve en la celda: el DNI 40011019 sale "40011019", no "4.0011019E7"
        return formateador.formatCellValue(celda, evaluador);
    }
}
