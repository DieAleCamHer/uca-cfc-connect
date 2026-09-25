package edu.udb.ucacfc.academico;

public record DocenteResponseDTO(Long id, String nombre, String especialidad, String correo, String telefono) {
    public static DocenteResponseDTO desde(Docente d) {
        return new DocenteResponseDTO(d.getId(), d.getNombre(), d.getEspecialidad(), d.getCorreo(), d.getTelefono());
    }
}
