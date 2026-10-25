package edu.udb.ucacfc.cliente.dto;

import edu.udb.ucacfc.shared.util.Patrones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lo que un CLIENTE puede editar de su propia ficha (el tipo y el DUI/NIT solo los corrige el personal). */
public record ClientePerfilRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150)
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, apostrofes, puntos y guiones")
        String nombre,
        @Size(max = 150) String empresa,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150) String correo,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = Patrones.TELEFONO_SV, message = "El telefono debe tener el formato ####-#### e iniciar con 2, 6 o 7")
        String telefono,
        @Size(max = 250) String direccion) {
}
