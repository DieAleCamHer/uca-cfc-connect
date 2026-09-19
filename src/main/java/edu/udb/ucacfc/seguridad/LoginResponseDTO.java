package edu.udb.ucacfc.seguridad;

public record LoginResponseDTO(String token, String tipo, Long usuarioId, String nombre, Rol rol) {
    public static LoginResponseDTO desde(String token, Usuario usuario) {
        return new LoginResponseDTO(token, "Bearer", usuario.getId(), usuario.getNombre(), usuario.getRol());
    }
}
