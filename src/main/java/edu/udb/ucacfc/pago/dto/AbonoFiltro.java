package edu.udb.ucacfc.pago.dto;

import edu.udb.ucacfc.pago.EstadoAbono;
import edu.udb.ucacfc.pago.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AbonoFiltro(
        @Schema(description = "PENDIENTE_VALIDACION = bandeja de Contabilidad") EstadoAbono estado,
        MetodoPago metodo,
        Long pagoId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
}
