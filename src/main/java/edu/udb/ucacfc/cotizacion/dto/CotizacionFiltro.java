package edu.udb.ucacfc.cotizacion.dto;

import edu.udb.ucacfc.cotizacion.EstadoCotizacion;
import edu.udb.ucacfc.cotizacion.TipoCotizacion;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CotizacionFiltro(
        @Schema(description = "Ignorado para CLIENTE") Long clienteId,
        TipoCotizacion tipo,
        EstadoCotizacion estado,
        @Schema(description = "Texto en la descripcion") String q,
        @Schema(description = "Solicitadas desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
}
