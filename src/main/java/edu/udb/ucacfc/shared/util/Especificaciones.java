package edu.udb.ucacfc.shared.util;

import edu.udb.ucacfc.shared.web.CatalogoFiltro;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Especificaciones (filtros dinamicos JPA Criteria) reutilizables. */
public final class Especificaciones {
    private Especificaciones() {
    }

    /** q busca (contiene, sin mayusculas) en los campos indicados; activo filtra por la columna "activo". */
    public static <T> Specification<T> catalogo(CatalogoFiltro f, String... camposTexto) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            String texto = f == null ? null : Textos.vacioANull(f.q());
            if (texto != null && camposTexto.length > 0) {
                String like = "%" + texto.toLowerCase() + "%";
                List<Predicate> or = new ArrayList<>();
                for (String c : camposTexto) {
                    or.add(cb.like(cb.lower(root.get(c)), like));
                }
                p.add(cb.or(or.toArray(Predicate[]::new)));
            }
            if (f != null && f.activo() != null) {
                p.add(cb.equal(root.get("activo"), f.activo()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
