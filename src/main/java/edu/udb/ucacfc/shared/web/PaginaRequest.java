package edu.udb.ucacfc.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parametros de paginacion como query params simples (?page=0&size=10&sort=nombre,asc).
 * Se recibe con @ParameterObject para que Swagger muestre tres cajas de texto y no un objeto JSON.
 */
public record PaginaRequest(
        @Schema(description = "Pagina, empieza en 0", defaultValue = "0", example = "0") Integer page,
        @Schema(description = "Registros por pagina (1-100)", defaultValue = "10", example = "10") Integer size,
        @Schema(description = "campo,asc|desc. Los campos validos se indican en cada endpoint", example = "id,desc")
        String sort) {
}
