package edu.udb.ucacfc.academico;

public record CategoriaResponseDTO(Long id, String nombre, String descripcion) {
    public static CategoriaResponseDTO desde(Categoria c) {
        return new CategoriaResponseDTO(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
