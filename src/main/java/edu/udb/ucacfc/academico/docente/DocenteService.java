package edu.udb.ucacfc.academico.docente;

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
 * Docentes. Correo y telefono unicos (identifican a la persona; dos homonimos si pueden existir).
 * No se desactiva un docente que tiene ofertas vigentes: primero hay que reasignarlas.
 */
@Service
@RequiredArgsConstructor
public class DocenteService {

    private static final Map<String, String> SORT = Map.of("id", "id", "nombre", "nombre",
            "especialidad", "especialidad", "correo", "correo", "activo", "activo");

    private final DocenteRepository repo;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<DocenteResponse> listar(CatalogoFiltro f, PaginaRequest pagina) {
        return PageResponse.of(repo.findAll(Especificaciones.<Docente>catalogo(f, "nombre", "especialidad", "correo"),
                PaginacionUtil.crear(pagina, SORT, "nombre,asc")).map(DocenteResponse::de));
    }

    @Transactional(readOnly = true)
    public DocenteResponse obtener(Long id) {
        return DocenteResponse.de(buscar(id));
    }

    @Transactional
    public DocenteResponse crear(DocenteRequest r) {
        validarUnicos(r, null);
        return DocenteResponse.de(repo.save(Docente.builder().nombre(r.nombre()).especialidad(r.especialidad())
                .correo(r.correo().toLowerCase()).telefono(vacio(r.telefono())).build()));
    }

    @Transactional
    public DocenteResponse actualizar(Long id, DocenteRequest r) {
        Docente d = buscar(id);
        validarUnicos(r, id);
        d.setNombre(r.nombre());
        d.setEspecialidad(r.especialidad());
        d.setCorreo(r.correo().toLowerCase());
        d.setTelefono(vacio(r.telefono()));
        return DocenteResponse.de(repo.save(d));
    }

    @Transactional
    public void desactivar(Long id) {
        Docente d = buscar(id);
        if (!d.isActivo()) {
            throw new ConflictoEstadoException("El docente ya esta desactivado");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        long vigentes = cursoRepository.countByDocenteIdAndActivoTrueAndFinAfter(id, ahora)
                + diplomadoRepository.countByDocenteIdAndActivoTrueAndFinAfter(id, ahora);
        if (vigentes > 0) {
            throw new ConflictoEstadoException("El docente '" + d.getNombre() + "' tiene " + vigentes
                    + " oferta(s) vigente(s). Reasignalas a otro docente antes de desactivarlo.");
        }
        d.setActivo(false);
        repo.save(d);
    }

    @Transactional
    public void activar(Long id) {
        Docente d = buscar(id);
        if (d.isActivo()) {
            throw new ConflictoEstadoException("El docente ya esta activo");
        }
        d.setActivo(true);
        repo.save(d);
    }

    public Docente buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Docente", id));
    }

    private void validarUnicos(DocenteRequest r, Long id) {
        boolean correo = id == null ? repo.existsByCorreoIgnoreCase(r.correo())
                : repo.existsByCorreoIgnoreCaseAndIdNot(r.correo(), id);
        if (correo) {
            throw new RegistroDuplicadoException("Ya existe un docente con el correo " + r.correo());
        }
        String tel = vacio(r.telefono());
        if (tel != null) {
            boolean t = id == null ? repo.existsByTelefono(tel) : repo.existsByTelefonoAndIdNot(tel, id);
            if (t) {
                throw new RegistroDuplicadoException("Ya existe un docente con el telefono " + tel);
            }
        }
    }

    private String vacio(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
