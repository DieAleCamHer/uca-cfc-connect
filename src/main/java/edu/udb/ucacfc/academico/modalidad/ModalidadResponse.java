package edu.udb.ucacfc.academico.modalidad;

public record ModalidadResponse(Long id, String nombre, boolean requiereEspacioFisico, boolean activo) {
    public static ModalidadResponse de(Modalidad m) {
        return new ModalidadResponse(m.getId(), m.getNombre(), m.isRequiereEspacioFisico(), m.isActivo());
    }
}
