package edu.udb.ucacfc.inscripcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Exactamente uno de cursoId / diplomadoId. Participante: obligatorio si el cliente es EMPRESA (cada colaborador
 * es una inscripcion); prohibido si es PERSONA (se inscribe a si misma).
 */
public record InscripcionRequest(
        @Schema(description = "Obligatorio para el personal; un CLIENTE siempre se inscribe a su nombre (se ignora)", example = "1")
        @Positive Long clienteId,
        @Schema(example = "1") @Positive Long cursoId,
        @Positive Long diplomadoId,
        @Schema(description = "Solo clientes EMPRESA", example = "Jose Martinez") @Size(max = 150) String participanteNombre,
        @Schema(description = "Solo clientes EMPRESA", example = "04567890-1")
        @Pattern(regexp = "^\\d{8}-\\d$", message = "El DUI del participante debe tener el formato ########-#")
        String participanteDui,
        @Schema(description = "Solo clientes EMPRESA") @Email(message = "El correo del participante no es valido")
        String participanteCorreo) {
}
