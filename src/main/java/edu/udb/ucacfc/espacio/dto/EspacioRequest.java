package edu.udb.ucacfc.espacio.dto;

import edu.udb.ucacfc.espacio.TipoEspacio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EspacioRequest(
        @Schema(example = "Auditorio Ignacio Ellacuria") @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @Schema(example = "AUDITORIO") @NotNull(message = "El tipo es obligatorio") TipoEspacio tipo,
        @Schema(example = "200") @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser mayor a cero") @Max(value = 2000, message = "Capacidad maxima 2000") Integer capacidad,
        @Schema(example = "75.00") @NotNull(message = "El precio por hora es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite maximo 2 decimales") BigDecimal precioHora,
        @Schema(example = "Proyector, sonido, aire acondicionado") @Size(max = 300) String equipamiento,
        @Schema(example = "Edificio CFC, primer nivel") @Size(max = 150) String ubicacion) {
}
