package edu.udb.ucacfc.academico.docente;

public record DocenteResponse(Long id, String nombre, String especialidad, String correo, String telefono,
                              boolean activo) {
    public static DocenteResponse de(Docente d) {
        return new DocenteResponse(d.getId(), d.getNombre(), d.getEspecialidad(), d.getCorreo(), d.getTelefono(),
                d.isActivo());
    }
}
