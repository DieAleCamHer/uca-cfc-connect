package edu.udb.ucacfc.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Respuesta paginada estable (no se serializa PageImpl de Spring, cuyo formato JSON no esta garantizado).
 */
@Schema(description = "Pagina de resultados")
public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Numero de pagina (empieza en 0)") int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        @Schema(example = "fechaInicio,desc") String sort) {

    public static <T> PageResponse<T> of(Page<T> p) {
        String sort = p.getSort().isSorted()
                ? p.getSort().stream().map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase())
                    .reduce((a, b) -> a + ";" + b).orElse(null)
                : null;
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(),
                p.getTotalPages(), p.isFirst(), p.isLast(), sort);
    }
}
