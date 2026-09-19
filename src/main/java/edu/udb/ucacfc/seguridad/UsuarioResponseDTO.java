package edu.udb.ucacfc.seguridad;

// Nunca incluye el password (ni siquiera el hash): no tiene por que
// salir de la capa de persistencia hacia el cliente de la API.
public record UsuarioResponseDTO(
        Long id,
        String nombre,
        String email,
        Rol rol,
        boolean activo
) {
    public static UsuarioResponseDTO desde(Usuario u) {
        return new UsuarioResponseDTO(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo());
    }
}
