package edu.udb.ucacfc.cliente.dto;

import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.shared.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Ficha de cliente registrada por el personal (cliente de mostrador, persona o empresa). */
public record ClienteRequest(
        @Schema(example = "EMPRESA") @NotNull(message = "El tipo de cliente es obligatorio") TipoCliente tipo,
        @Schema(example = "0614-010190-101-3") @NotBlank(message = "El DUI/NIT es obligatorio")
        @Pattern(regexp = Patrones.DUI_NIT, message = "DUI ########-# o NIT ####-######-###-# (o 14 digitos)") String duiNit,
        @Schema(description = "Persona, o contacto de la empresa", example = "Carlos Mendez") @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, apostrofes, puntos y guiones")
        String nombre,
        @Schema(description = "Razon social; obligatoria si tipo = EMPRESA", example = "Inversiones Delta, S.A. de C.V.")
        @Size(max = 150) String empresa,
        @Schema(example = "rrhh@delta.com.sv") @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido") @Size(max = 150) String correo,
        @Schema(example = "2250-1000")
        @Pattern(regexp = Patrones.TELEFONO_SV, message = "El telefono debe tener el formato ####-#### e iniciar con 2, 6 o 7")
        String telefono,
        @Schema(example = "Boulevard Los Proceres, San Salvador") @Size(max = 250) String direccion) {
}
