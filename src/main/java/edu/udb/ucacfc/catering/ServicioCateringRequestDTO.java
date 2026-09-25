package edu.udb.ucacfc.catering;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ServicioCateringRequestDTO(
        @NotNull(message = "El tipo es obligatorio") TipoServicioCatering tipo,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull @DecimalMin(value = "0.0", message = "El precio unitario no puede ser negativo") BigDecimal precioUnitario
) {
}
