package edu.udb.ucacfc.pago;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AbonoRequestDTO(
        @NotNull @Positive(message = "El monto abonado debe ser mayor a cero") BigDecimal montoAbonado
) {
}
