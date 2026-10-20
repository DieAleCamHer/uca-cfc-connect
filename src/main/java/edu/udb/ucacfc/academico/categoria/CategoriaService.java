package edu.udb.ucacfc.academico.categoria;

import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.util.Especificaciones;
import edu.udb.ucacfc.shared.web.CatalogoFiltro;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import edu.udb.ucacfc.shared.web.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Categorias. Desactivar NO afecta las ofertas existentes: solo impide usarla en ofertas nuevas
 * o al cambiar la categoria de una oferta.
 */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private static final Map<String, String> SORT = Map.of("id", "id", "nombre", "nombre", "activo", "activo");
    private final CategoriaRepository repo;

    @Transactional(readOnly = true)
    public PageResponse<CategoriaResponse> listar(CatalogoFiltro f, PaginaRequest pagina) {
        return PageResponse.of(repo.findAll(Especificaciones.<Categoria>catalogo(f, "nombre", "descripcion"),
                PaginacionUtil.crear(pagina, SORT, "nombre,asc")).map(CategoriaResponse::de));
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.de(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest r) {
        if (repo.existsByNombreIgnoreCase(r.nombre())) {
            throw new RegistroDuplicadoException("Ya existe una categoria llamada '" + r.nombre() + "'");
        }
        return CategoriaResponse.de(repo.save(Categoria.builder().nombre(r.nombre()).descripcion(r.descripcion()).build()));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest r) {
        Categoria c = buscar(id);
        if (repo.existsByNombreIgnoreCaseAndIdNot(r.nombre(), id)) {
            throw new RegistroDuplicadoException("Ya existe otra categoria llamada '" + r.nombre() + "'");
        }
        c.setNombre(r.nombre());
        c.setDescripcion(r.descripcion());
        return CategoriaResponse.de(repo.save(c));
    }

    @Transactional
    public void desactivar(Long id) {
        Categoria c = buscar(id);
        if (!c.isActivo()) {
            throw new ConflictoEstadoException("La categoria ya esta desactivada");
        }
        c.setActivo(false);
        repo.save(c);
    }

    @Transactional
    public void activar(Long id) {
        Categoria c = buscar(id);
        if (c.isActivo()) {
            throw new ConflictoEstadoException("La categoria ya esta activa");
        }
        c.setActivo(true);
        repo.save(c);
    }

    public Categoria buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Categoria", id));
    }
}
