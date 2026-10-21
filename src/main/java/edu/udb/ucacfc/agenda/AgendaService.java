package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.agenda.dto.ActividadResponse;
import edu.udb.ucacfc.agenda.dto.AgendaFiltro;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.exception.EspacioOcupadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.security.AccesoService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * AGENDA INSTITUCIONAL: el unico lugar donde se valida la regla "no podran existir conflictos de horario"
 * para los espacios. Cursos, diplomados, eventos, reservas y catering registran aqui lo que ocupan.
 *
 * registrar() corre dentro de la transaccion de quien llama (MANDATORY): si un curso de 8 sesiones choca en la
 * sesion 5, se revierte tambien el curso y las sesiones 1-4. Antes de buscar cruces bloquea la fila del espacio
 * (SELECT ... FOR UPDATE) para que dos peticiones simultaneas no tomen el mismo horario.
 */
@Service
@RequiredArgsConstructor
public class AgendaService {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<TipoActividad> PUBLICAS = EnumSet.of(TipoActividad.CURSO, TipoActividad.DIPLOMADO,
            TipoActividad.EVENTO);
    private static final long MAX_DIAS_CONSULTA = 93;

    private final ActividadAgendaRepository repo;
    private final EspacioRepository espacioRepository;
    private final AccesoService acceso;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(NuevaActividad a) {
        if (!a.fin().isAfter(a.inicio())) {
            throw new ReglaNegocioException("La actividad debe terminar despues de iniciar");
        }
        Espacio espacio = a.espacio();
        if (espacio != null && a.bloqueaEspacio()) {
            espacioRepository.bloquear(espacio.getId());
            List<ActividadAgenda> cruces = repo.cruces(espacio.getId(), a.inicio(), a.fin());
            if (!cruces.isEmpty()) {
                ActividadAgenda c = cruces.get(0);
                throw new EspacioOcupadoException("El espacio '" + espacio.getNombre() + "' ya esta ocupado entre "
                        + FMT.format(c.getInicio()) + " y " + FMT.format(c.getFin()) + " por " + c.getTitulo()
                        + ". Horario solicitado: " + FMT.format(a.inicio()) + " - " + FMT.format(a.fin()));
            }
        }
        repo.save(ActividadAgenda.builder()
                .tipo(a.tipo()).referenciaId(a.referenciaId()).titulo(a.titulo())
                .inicio(a.inicio()).fin(a.fin()).espacio(espacio).bloqueaEspacio(a.bloqueaEspacio())
                .clienteId(a.clienteId()).detalle(a.detalle())
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarTodas(List<NuevaActividad> actividades) {
        actividades.forEach(this::registrar);
    }

    /** Libera el espacio (cancelaciones, bajas y reprogramaciones). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void eliminar(TipoActividad tipo, Long referenciaId) {
        repo.eliminar(tipo, referenciaId);
    }

    /** Primer conflicto (si existe) ignorando las actividades del propio registro que se esta editando. */
    @Transactional(readOnly = true)
    public Optional<ActividadAgenda> conflicto(Long espacioId, LocalDateTime inicio, LocalDateTime fin,
                                               TipoActividad excluirTipo, Long excluirRef) {
        return repo.cruces(espacioId, inicio, fin).stream()
                .filter(a -> excluirTipo == null || !(a.getTipo() == excluirTipo && a.getReferenciaId().equals(excluirRef)))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public long actividadesFuturas(Long espacioId, LocalDateTime ahora) {
        return repo.countByEspacioIdAndBloqueaEspacioTrueAndFinAfter(espacioId, ahora);
    }

    /**
     * Calendario. Rango obligatorio de hasta 93 dias. Un CLIENTE ve las actividades publicas (cursos, diplomados,
     * eventos) y solo SUS reservas y catering; las de otros clientes no se le muestran.
     */
    @Transactional(readOnly = true)
    public List<ActividadResponse> consultar(AgendaFiltro f, Usuario usuario) {
        if (f.desde() == null || f.hasta() == null) {
            throw new ReglaNegocioException("Indica el rango 'desde' y 'hasta' (AAAA-MM-DD)");
        }
        if (f.hasta().isBefore(f.desde())) {
            throw new ReglaNegocioException("'hasta' no puede ser anterior a 'desde'");
        }
        if (Duration.between(f.desde().atStartOfDay(), f.hasta().atStartOfDay()).toDays() > MAX_DIAS_CONSULTA) {
            throw new ReglaNegocioException("El rango maximo de consulta es de " + MAX_DIAS_CONSULTA + " dias");
        }
        LocalDateTime desde = f.desde().atStartOfDay();
        LocalDateTime hasta = f.hasta().plusDays(1).atStartOfDay();
        Long clientePropio = acceso.esCliente(usuario) ? acceso.clienteIdDe(usuario) : null;

        Specification<ActividadAgenda> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.lessThan(root.get("inicio"), hasta));
            p.add(cb.greaterThan(root.get("fin"), desde));
            if (f.tipo() != null) {
                p.add(cb.equal(root.get("tipo"), f.tipo()));
            }
            if (f.espacioId() != null) {
                p.add(cb.equal(root.get("espacio").get("id"), f.espacioId()));
            }
            if (clientePropio != null) {
                p.add(cb.or(root.get("tipo").in(PUBLICAS), cb.equal(root.get("clienteId"), clientePropio)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return repo.findAll(spec, Sort.by("inicio")).stream().map(ActividadResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ActividadResponse obtener(Long id, Usuario usuario) {
        ActividadAgenda a = repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Actividad", id));
        if (!PUBLICAS.contains(a.getTipo())) {
            acceso.verificarDueno(usuario, a.getClienteId(), "una actividad");
        }
        return ActividadResponse.de(a);
    }
}
