package edu.udb.ucacfc.espacio.dto;

import edu.udb.ucacfc.espacio.TipoEspacio;

import java.math.BigDecimal;

/** libre = sin actividades que lo bloqueen en el rango consultado. conflicto describe la primera que choca. */
public record DisponibilidadResponse(Long espacioId, String nombre, TipoEspacio tipo, int capacidad,
                                     BigDecimal precioHora, BigDecimal costoEstimado, boolean libre, String conflicto) {
}
