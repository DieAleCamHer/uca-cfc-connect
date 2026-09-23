package edu.udb.ucacfc.cotizacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CotizacionRequestDTO(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotNull(message = "El tipo es obligatorio") TipoCotizacion tipo,
        String descripcion,
        @NotNull @DecimalMin(value = "0.0", message = "El total no puede ser negativo") BigDecimal total
) {
}
