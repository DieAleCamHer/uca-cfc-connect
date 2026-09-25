package edu.udb.ucacfc.pago;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PagoRequestDTO(
        @NotNull(message = "El tipo de referencia es obligatorio") TipoReferenciaPago tipoReferencia,
        @NotNull(message = "El id de referencia es obligatorio") Long referenciaId,
        @NotNull @Positive(message = "El monto debe ser mayor a cero") BigDecimal monto,
        @NotNull(message = "El metodo de pago es obligatorio") MetodoPago metodo
) {
}
