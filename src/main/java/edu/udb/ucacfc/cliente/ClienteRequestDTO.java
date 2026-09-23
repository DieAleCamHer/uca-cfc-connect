package edu.udb.ucacfc.cliente;

import jakarta.validation.constraints.NotBlank;

public record ClienteRequestDTO(
        @NotBlank(message = "El DUI/NIT es obligatorio") String duiNit,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String empresa,
        @NotBlank(message = "El correo es obligatorio") String correo,
        String telefono,
        String direccion
) {
}
