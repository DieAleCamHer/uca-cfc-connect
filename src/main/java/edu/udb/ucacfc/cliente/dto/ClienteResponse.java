package edu.udb.ucacfc.cliente.dto;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.TipoCliente;

import java.time.LocalDateTime;

public record ClienteResponse(Long id, TipoCliente tipo, String duiNit, String nombre, String empresa,
                              String nombreMostrado, String correo, String telefono, String direccion,
                              boolean activo, Long usuarioId, String usuarioEmail, LocalDateTime fechaCreacion) {
    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getTipo(), c.getDuiNit(), c.getNombre(), c.getEmpresa(),
                c.nombreMostrado(), c.getCorreo(), c.getTelefono(), c.getDireccion(), c.isActivo(),
                c.getUsuario() == null ? null : c.getUsuario().getId(),
                c.getUsuario() == null ? null : c.getUsuario().getEmail(), c.getFechaCreacion());
    }
}
