package edu.udb.ucacfc.pago.dto;

import edu.udb.ucacfc.pago.EstadoPago;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record PagoFiltro(
        EstadoPago estado,
        TipoReferenciaPago tipoReferencia,
        @Schema(description = "Ignorado para CLIENTE (siempre ve solo lo suyo)") Long clienteId,
        @Schema(description = "Generadas desde (AAAA-MM-DD)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @Schema(description = "Generadas hasta (AAAA-MM-DD)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @Schema(description = "Texto en el concepto") String q,
        @Schema(description = "true = solo con saldo pendiente") Boolean conSaldo) {
}
