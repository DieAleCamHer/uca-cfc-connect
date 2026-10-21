package edu.udb.ucacfc.shared.web;

import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Convierte PaginaRequest en un Pageable REAL validando:
 *  - page >= 0, 1 <= size <= 100
 *  - sort solo por campos de una LISTA BLANCA (alias publico -> propiedad JPA). Asi nadie ordena por
 *    "password" ni recibe un 500 por un campo inexistente, y se permiten alias como cliente -> cliente.nombre.
 */
public final class PaginacionUtil {

    public static final int MAX_SIZE = 100;

    private PaginacionUtil() {
    }

    public static Pageable crear(PaginaRequest req, Map<String, String> camposPermitidos, String sortPorDefecto) {
        int page = req == null || req.page() == null ? 0 : req.page();
        int size = req == null || req.size() == null ? 10 : req.size();
        if (page < 0) {
            throw new ReglaNegocioException("'page' no puede ser negativo");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ReglaNegocioException("'size' debe estar entre 1 y " + MAX_SIZE);
        }
        String sort = req == null || req.sort() == null || req.sort().isBlank() ? sortPorDefecto : req.sort();
        return PageRequest.of(page, size, construirSort(sort, camposPermitidos));
    }

    private static Sort construirSort(String sort, Map<String, String> permitidos) {
        Sort resultado = Sort.unsorted();
        // Se aceptan varios criterios separados por ';' -> "estado,asc;fechaInicio,desc"
        for (String criterio : sort.split(";")) {
            if (criterio.isBlank()) {
                continue;
            }
            String[] partes = criterio.split(",");
            String alias = partes[0].trim();
            String propiedad = permitidos.get(alias);
            if (propiedad == null) {
                throw new ReglaNegocioException("No se puede ordenar por '" + alias + "'. Campos validos: "
                        + permitidos.keySet());
            }
            Sort.Direction dir = Sort.Direction.ASC;
            if (partes.length > 1) {
                String d = partes[1].trim();
                if (d.equalsIgnoreCase("desc")) {
                    dir = Sort.Direction.DESC;
                } else if (!d.equalsIgnoreCase("asc")) {
                    throw new ReglaNegocioException("Direccion de orden invalida '" + d + "'. Usa asc o desc");
                }
            }
            resultado = resultado.and(Sort.by(dir, propiedad));
        }
        return resultado;
    }
}
