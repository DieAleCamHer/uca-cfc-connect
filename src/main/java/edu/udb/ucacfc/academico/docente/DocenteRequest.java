package edu.udb.ucacfc.academico.docente;

import edu.udb.ucacfc.shared.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocenteRequest(
        @Schema(example = "Ing. Maria Lopez") @NotBlank(message = "El nombre es obligatorio") @Size(max = 150)
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, apostrofes, puntos y guiones")
        String nombre,
        @Schema(example = "Desarrollo web con Java") @Size(max = 150) String especialidad,
        @Schema(example = "mlopez@ucacfc.edu.sv") @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido") @Size(max = 150) String correo,
        @Schema(example = "7123-4567")
        @Pattern(regexp = Patrones.TELEFONO_SV, message = "El telefono debe tener el formato ####-#### e iniciar con 2, 6 o 7")
        String telefono) {
}
