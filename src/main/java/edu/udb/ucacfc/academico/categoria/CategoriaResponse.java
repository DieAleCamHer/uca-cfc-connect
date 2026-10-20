package edu.udb.ucacfc.academico.categoria;

public record CategoriaResponse(Long id, String nombre, String descripcion, boolean activo) {
    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.isActivo());
    }
}
