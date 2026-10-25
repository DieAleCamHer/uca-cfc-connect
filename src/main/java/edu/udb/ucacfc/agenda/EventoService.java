package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.agenda.dto.EventoFiltro;
import edu.udb.ucacfc.agenda.dto.EventoRequest;
import edu.udb.ucacfc.agenda.dto.EventoResponse;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.util.Textos;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import edu.udb.ucacfc.shared.web.PaginacionUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Eventos institucionales: futuros, de 30 min a 12 h, en un espacio activo (lo bloquea) o en un lugar externo. */
@Service
@RequiredArgsConstructor
public class EventoService {

    private static final Map<String, String> SORT = Map.of("id", "id", "inicio", "inicio", "titulo", "titulo",
            "estado", "estado");

    private final EventoRepository repo;
    private final EspacioRepository espacioRepository;
    private final AgendaService agenda;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<EventoResponse> listar(EventoFiltro f, PaginaRequest pagina) {
        Specification<EventoInstitucional> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    p.add(cb.like(cb.lower(root.get("titulo")), "%" + texto.toLowerCase() + "%"));
                }
                if (f.estado() != null) {
                    p.add(cb.equal(root.get("estado"), f.estado()));
                }
                if (f.espacioId() != null) {
                    p.add(cb.equal(root.get("espacio").get("id"), f.espacioId()));
                }
                if (f.desde() != null) {
                    p.add(cb.greaterThan(root.get("fin"), f.desde().atStartOfDay()));
                }
                if (f.hasta() != null) {
                    p.add(cb.lessThan(root.get("inicio"), f.hasta().plusDays(1).atStartOfDay()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "inicio,asc")).map(EventoResponse::de));
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(Long id) {
        return EventoResponse.de(buscar(id));
    }

    @Transactional
    public EventoResponse crear(EventoRequest r) {
        EventoInstitucional e = EventoInstitucional.builder().estado(EstadoEvento.PROGRAMADO).build();
        aplicar(e, r);
        e = repo.save(e);
        registrar(e);
        return EventoResponse.de(e);
    }

    @Transactional
    public EventoResponse actualizar(Long id, EventoRequest r) {
        EventoInstitucional e = buscar(id);
        exigirModificable(e);
        agenda.eliminar(TipoActividad.EVENTO, id);
        aplicar(e, r);
        e = repo.save(e);
        registrar(e);
        return EventoResponse.de(e);
    }

    @Transactional
    public EventoResponse cancelar(Long id, String motivo) {
        EventoInstitucional e = buscar(id);
        exigirModificable(e);
        e.setEstado(EstadoEvento.CANCELADO);
        e.setMotivoCancelacion(motivo);
        agenda.eliminar(TipoActividad.EVENTO, id);
        return EventoResponse.de(repo.save(e));
    }

    private void exigirModificable(EventoInstitucional e) {
        if (e.getEstado() == EstadoEvento.CANCELADO) {
            throw new ConflictoEstadoException("El evento esta cancelado");
        }
        if (!e.getInicio().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictoEstadoException("El evento ya inicio: no se puede modificar");
        }
    }

    private void aplicar(EventoInstitucional e, EventoRequest r) {
        if (!r.inicio().isAfter(LocalDateTime.now(clock))) {
            throw new ReglaNegocioException("El evento debe programarse en el futuro");
        }
        if (!r.fin().isAfter(r.inicio())) {
            throw new ReglaNegocioException("El fin debe ser posterior al inicio");
        }
        Duration d = Duration.between(r.inicio(), r.fin());
        if (d.toMinutes() < 30 || d.toHours() > 12) {
            throw new ReglaNegocioException("Un evento dura entre 30 minutos y 12 horas");
        }
        Espacio espacio = null;
        if (r.espacioId() != null) {
            espacio = espacioRepository.findById(r.espacioId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", r.espacioId()));
            if (!espacio.isActivo()) {
                throw new ReglaNegocioException("El espacio '" + espacio.getNombre() + "' esta desactivado");
            }
            if (r.asistentesEstimados() != null && r.asistentesEstimados() > espacio.getCapacidad()) {
                throw new ReglaNegocioException("El espacio '" + espacio.getNombre() + "' tiene capacidad para "
                        + espacio.getCapacidad() + " personas");
            }
        } else if (Textos.vacioANull(r.lugarExterno()) == null) {
            throw new ReglaNegocioException("Indica un espacio del CFC o el lugar externo / enlace virtual");
        }
        e.setTitulo(r.titulo());
        e.setDescripcion(r.descripcion());
        e.setInicio(r.inicio().withSecond(0).withNano(0));
        e.setFin(r.fin().withSecond(0).withNano(0));
        e.setEspacio(espacio);
        e.setLugarExterno(espacio == null ? r.lugarExterno() : null);
        e.setAsistentesEstimados(r.asistentesEstimados());
        e.setResponsable(r.responsable());
    }

    private void registrar(EventoInstitucional e) {
        agenda.registrar(new NuevaActividad(TipoActividad.EVENTO, e.getId(), "Evento: " + e.getTitulo(), e.getInicio(),
                e.getFin(), e.getEspacio(), e.getEspacio() != null, null,
                e.getEspacio() == null ? e.getLugarExterno() : "Responsable: " + e.getResponsable()));
    }

    private EventoInstitucional buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Evento", id));
    }
}
