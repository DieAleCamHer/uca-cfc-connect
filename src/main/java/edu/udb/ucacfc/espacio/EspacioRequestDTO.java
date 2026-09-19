package edu.udb.ucacfc.espacio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record EspacioRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El tipo es obligatorio") TipoEspacio tipo,
        @NotNull @Positive(message = "La capacidad debe ser mayor a cero") Integer capacidad,
        @NotNull @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") BigDecimal precio,
        String equipamiento
) {
}
