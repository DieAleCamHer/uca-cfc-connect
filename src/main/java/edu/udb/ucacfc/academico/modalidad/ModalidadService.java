package edu.udb.ucacfc.academico.modalidad;

import edu.udb.ucacfc.academico.oferta.CursoRepository;
import edu.udb.ucacfc.academico.oferta.DiplomadoRepository;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Regla agregada para coherencia: no se puede cambiar "requiereEspacioFisico" si hay ofertas vigentes con esa
 * modalidad (quedarian cursos virtuales con aula o presenciales sin aula).
 */
@Service
@RequiredArgsConstructor
public class ModalidadService {

    private static final Map<String, String> SORT = Map.of("id", "id", "nombre", "nombre",
            "requiereEspacioFisico", "requiereEspacioFisico", "activo", "activo");

    private final ModalidadRepository repo;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<ModalidadResponse> listar(CatalogoFiltro f, PaginaRequest pagina) {
        return PageResponse.of(repo.findAll(Especificaciones.<Modalidad>catalogo(f, "nombre"),
                PaginacionUtil.crear(pagina, SORT, "nombre,asc")).map(ModalidadResponse::de));
    }

    @Transactional(readOnly = true)
    public ModalidadResponse obtener(Long id) {
        return ModalidadResponse.de(buscar(id));
    }

    @Transactional
    public ModalidadResponse crear(ModalidadRequest r) {
        if (repo.existsByNombreIgnoreCase(r.nombre())) {
            throw new RegistroDuplicadoException("Ya existe una modalidad llamada '" + r.nombre() + "'");
        }
        return ModalidadResponse.de(repo.save(Modalidad.builder().nombre(r.nombre().toUpperCase())
                .requiereEspacioFisico(r.requiereEspacioFisico()).build()));
    }

    @Transactional
    public ModalidadResponse actualizar(Long id, ModalidadRequest r) {
        Modalidad m = buscar(id);
        if (repo.existsByNombreIgnoreCaseAndIdNot(r.nombre(), id)) {
            throw new RegistroDuplicadoException("Ya existe otra modalidad llamada '" + r.nombre() + "'");
        }
        if (m.isRequiereEspacioFisico() != r.requiereEspacioFisico()) {
            long vigentes = vigentes(id);
            if (vigentes > 0) {
                throw new ConflictoEstadoException("No se puede cambiar si la modalidad requiere espacio fisico: hay "
                        + vigentes + " oferta(s) vigente(s) que la usan. Crea una modalidad nueva.");
            }
        }
        m.setNombre(r.nombre().toUpperCase());
        m.setRequiereEspacioFisico(r.requiereEspacioFisico());
        return ModalidadResponse.de(repo.save(m));
    }

    @Transactional
    public void desactivar(Long id) {
        Modalidad m = buscar(id);
        if (!m.isActivo()) {
            throw new ConflictoEstadoException("La modalidad ya esta desactivada");
        }
        m.setActivo(false);
        repo.save(m);
    }

    @Transactional
    public void activar(Long id) {
        Modalidad m = buscar(id);
        if (m.isActivo()) {
            throw new ConflictoEstadoException("La modalidad ya esta activa");
        }
        m.setActivo(true);
        repo.save(m);
    }

    public Modalidad buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Modalidad", id));
    }

    private long vigentes(Long id) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return cursoRepository.countByModalidadIdAndActivoTrueAndFinAfter(id, ahora)
                + diplomadoRepository.countByModalidadIdAndActivoTrueAndFinAfter(id, ahora);
    }
}
