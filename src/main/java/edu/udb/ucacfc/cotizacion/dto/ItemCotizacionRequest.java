package edu.udb.ucacfc.cotizacion.dto;

import edu.udb.ucacfc.cotizacion.TipoItemCotizacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemCotizacionRequest(
        @Schema(example = "CURSO") @NotNull(message = "El tipo de linea es obligatorio") TipoItemCotizacion tipoItem,
        @Schema(description = "id del curso / diplomado / espacio / servicio de catering. Vacio en OTRO", example = "1")
        @Positive Long referenciaId,
        @Schema(description = "Obligatoria en OTRO; opcional en las demas", example = "Grupo cerrado para 20 colaboradores")
        @Size(max = 250) String descripcion,
        @Schema(description = "CURSO/DIPLOMADO: participantes; ESPACIO: horas; CATERING: asistentes; OTRO: unidades", example = "20")
        @NotNull(message = "La cantidad es obligatoria") @Positive(message = "La cantidad debe ser mayor a cero")
        @Max(value = 10000) Integer cantidad,
        @Schema(description = "Solo el personal puede fijarlo (precio negociado). Para un CLIENTE se usa el precio de catalogo.")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo") @Digits(integer = 8, fraction = 2)
        BigDecimal precioUnitario) {
}
