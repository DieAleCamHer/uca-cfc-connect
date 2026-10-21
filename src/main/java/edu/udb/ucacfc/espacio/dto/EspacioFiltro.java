package edu.udb.ucacfc.espacio.dto;

import edu.udb.ucacfc.espacio.TipoEspacio;
import io.swagger.v3.oas.annotations.media.Schema;

public record EspacioFiltro(
        @Schema(description = "Texto en nombre, equipamiento o ubicacion") String q,
        TipoEspacio tipo,
        Boolean activo,
        @Schema(description = "Capacidad minima requerida") Integer capacidadMinima) {
}
