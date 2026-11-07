package com.clubimperio.gestionclub.membresia.controllers;

import com.clubimperio.gestionclub.membresia.dtos.ImportacionResultado;
import com.clubimperio.gestionclub.membresia.services.ImportacionPersonasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// Solo ADMIN (ver SecurityConfig): una carga masiva puede dar de alta miles de personas de una vez.
@RestController
@RequestMapping("/api/personas/importar")
@RequiredArgsConstructor
public class ImportacionPersonaController {
    private final ImportacionPersonasService importacionService;

    // multipart/form-data con el campo "archivo" (.csv o .xlsx).
    // ?simular=true valida y devuelve el reporte sin guardar nada (vista previa).
    // required = false: si falta el archivo, el service responde con un mensaje propio en vez del error genérico de Spring.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionResultado> importar(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(defaultValue = "false") boolean simular
    ) {
        return ResponseEntity.ok(importacionService.importar(archivo, simular));
    }

    // Descarga de un CSV de ejemplo con las columnas esperadas
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> descargarPlantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("plantilla-socios.csv").build().toString())
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(importacionService.plantillaCsv());
    }
}
