package edu.udb.ucacfc.academico;

public record ModalidadResponseDTO(Long id, String nombre, boolean requiereEspacioFisico) {
    public static ModalidadResponseDTO desde(Modalidad m) {
        return new ModalidadResponseDTO(m.getId(), m.getNombre(), m.isRequiereEspacioFisico());
    }
}
