package edu.udb.ucacfc.catering.dto;

import edu.udb.ucacfc.catering.TipoServicioCatering;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioCateringRequest(
        @Schema(example = "ALMUERZO") @NotNull(message = "El tipo es obligatorio") TipoServicioCatering tipo,
        @Schema(example = "Almuerzo ejecutivo") @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @Schema(example = "Pollo en salsa, arroz, ensalada, postre y bebida") @Size(max = 500) String menuBase,
        @Schema(example = "8.50") @NotNull(message = "El precio por persona es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio por persona debe ser mayor a cero")
        @Digits(integer = 8, fraction = 2) BigDecimal precioUnitario) {
}
