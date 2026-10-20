package edu.udb.ucacfc.seguridad.auth.dto;

import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.shared.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registro publico: crea la cuenta (rol CLIENTE, siempre) y su ficha de cliente en una sola transaccion.
 * No existe campo "rol": si alguien lo envia, Jackson responde 400 (fail-on-unknown-properties).
 */
public record RegistroRequest(
        @Schema(example = "Ana Lucia Perez") @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite maximo 120 caracteres")
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, apostrofes, puntos y guiones")
        String nombre,
        @Schema(example = "ana.perez@correo.com") @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido") @Size(max = 150) String email,
        @Schema(example = "Clave2026") @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(regexp = Patrones.PASSWORD, message = Patrones.MSG_PASSWORD) String password,
        @Schema(example = "PERSONA") @NotNull(message = "El tipo de cliente es obligatorio") TipoCliente tipo,
        @Schema(example = "01234567-8") @NotBlank(message = "El DUI/NIT es obligatorio")
        @Pattern(regexp = Patrones.DUI_NIT, message = "DUI ########-# o NIT ####-######-###-# (o 14 digitos)") String duiNit,
        @Schema(description = "Obligatorio si tipo = EMPRESA", example = "Inversiones Delta, S.A. de C.V.")
        @Size(max = 150) String empresa,
        @Schema(example = "7123-4567") @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = Patrones.TELEFONO_SV, message = "El telefono debe tener el formato ####-#### e iniciar con 2, 6 o 7")
        String telefono,
        @Schema(example = "Colonia Escalon, San Salvador") @Size(max = 250) String direccion) {
}
