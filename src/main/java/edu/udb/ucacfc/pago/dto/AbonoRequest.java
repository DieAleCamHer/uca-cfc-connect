package edu.udb.ucacfc.pago.dto;

import edu.udb.ucacfc.pago.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AbonoRequest(
        @Schema(example = "50.00") @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite maximo 2 decimales") BigDecimal monto,
        @Schema(example = "TRANSFERENCIA") @NotNull(message = "El metodo de pago es obligatorio") MetodoPago metodo,
        @Schema(description = "Obligatorio en TRANSFERENCIA y DEPOSITO", example = "TRX-20261017-0001")
        @Size(max = 60) String referenciaExterna) {
}
