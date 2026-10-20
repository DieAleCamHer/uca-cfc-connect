package edu.udb.ucacfc.seguridad.usuario.dto;

import edu.udb.ucacfc.seguridad.usuario.Rol;

import java.util.List;

public record RolResponse(Rol codigo, String nombre, String descripcion, List<String> permisos,
                          boolean personal, long usuariosActivos) {
}
