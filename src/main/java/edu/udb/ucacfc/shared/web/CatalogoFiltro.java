package edu.udb.ucacfc.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** Filtro comun de catalogos: texto libre y estado. */
public record CatalogoFiltro(
        @Schema(description = "Texto a buscar en el nombre") String q,
        @Schema(description = "true = solo activos, false = solo inactivos, vacio = todos") Boolean activo) {
}
