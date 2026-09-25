package edu.udb.ucacfc.cliente;

public record ClienteResponseDTO(
        Long id,
        String duiNit,
        String nombre,
        String empresa,
        String correo,
        String telefono,
        String direccion
) {
    public static ClienteResponseDTO desde(Cliente c) {
        return new ClienteResponseDTO(c.getId(), c.getDuiNit(), c.getNombre(), c.getEmpresa(),
                c.getCorreo(), c.getTelefono(), c.getDireccion());
    }
}
