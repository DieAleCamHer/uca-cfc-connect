package edu.udb.ucacfc.cotizacion.dto;

import edu.udb.ucacfc.cotizacion.TipoCotizacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CotizacionRequest(
        @Schema(description = "Obligatorio para el personal; un CLIENTE siempre cotiza a su nombre", example = "1")
        @Positive Long clienteId,
        @Schema(example = "SERVICIO_COMBINADO") @NotNull(message = "El tipo es obligatorio") TipoCotizacion tipo,
        @Schema(example = "Capacitacion de 20 colaboradores con almuerzo") @NotBlank(message = "Describe lo que necesitas")
        @Size(max = 500) String descripcion,
        @Schema(description = "Fecha tentativa del servicio", example = "2026-12-05") LocalDate fechaEvento,
        @NotEmpty(message = "Agrega al menos una linea") @Size(max = 30, message = "Maximo 30 lineas")
        List<@Valid @NotNull ItemCotizacionRequest> items,
        @Schema(description = "Solo personal") @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal descuento,
        @Schema(description = "Solo personal; por defecto hoy + 15 dias") LocalDate fechaVencimiento) {
}
