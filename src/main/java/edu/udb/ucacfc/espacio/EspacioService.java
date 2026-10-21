package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.academico.oferta.CursoRepository;
import edu.udb.ucacfc.academico.oferta.DiplomadoRepository;
import edu.udb.ucacfc.academico.oferta.OfertaAcademica;
import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.catering.EstadoSolicitudCatering;
import edu.udb.ucacfc.catering.SolicitudCateringRepository;
import edu.udb.ucacfc.espacio.dto.DisponibilidadFiltro;
import edu.udb.ucacfc.espacio.dto.DisponibilidadResponse;
import edu.udb.ucacfc.espacio.dto.EspacioFiltro;
import edu.udb.ucacfc.espacio.dto.EspacioRequest;
import edu.udb.ucacfc.espacio.dto.EspacioResponse;
import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacioRepository;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.util.Horarios;
import edu.udb.ucacfc.shared.util.Textos;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import edu.udb.ucacfc.shared.web.PaginacionUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Modulo 5 - Espacios (capacidad, precio, disponibilidad, equipamiento).
 * Reglas agregadas para coherencia:
 *  - La capacidad no puede bajar de lo que ya esta comprometido (cupo de ofertas vigentes o asistentes de
 *    reservas/catering vigentes en ese espacio).
 *  - No se desactiva un espacio con actividades futuras en la agenda.
 *  - El nuevo precio por hora solo aplica a reservas nuevas (cada reserva congela el suyo).
 */
@Service
@RequiredArgsConstructor
public class EspacioService {

    private static final Map<String, String> SORT = Map.of("id", "id", "nombre", "nombre", "tipo", "tipo",
            "capacidad", "capacidad", "precioHora", "precioHora", "activo", "activo");

    private final EspacioRepository repo;
    private final AgendaService agenda;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final ReservaEspacioRepository reservaRepository;
    private final SolicitudCateringRepository cateringRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<EspacioResponse> listar(EspacioFiltro f, PaginaRequest pagina) {
        return PageResponse.of(repo.findAll(spec(f), PaginacionUtil.crear(pagina, SORT, "nombre,asc"))
                .map(EspacioResponse::de));
    }

    @Transactional(readOnly = true)
    public EspacioResponse obtener(Long id) {
        return EspacioResponse.de(buscar(id));
    }

    @Transactional
    public EspacioResponse crear(EspacioRequest r) {
        if (repo.existsByNombreIgnoreCase(r.nombre())) {
            throw new RegistroDuplicadoException("Ya existe un espacio llamado '" + r.nombre() + "'");
        }
        Espacio e = Espacio.builder().nombre(r.nombre()).tipo(r.tipo()).capacidad(r.capacidad())
                .precioHora(r.precioHora()).equipamiento(r.equipamiento()).ubicacion(r.ubicacion()).build();
        return EspacioResponse.de(repo.save(e));
    }

    @Transactional
    public EspacioResponse actualizar(Long id, EspacioRequest r) {
        Espacio e = buscar(id);
        if (repo.existsByNombreIgnoreCaseAndIdNot(r.nombre(), id)) {
            throw new RegistroDuplicadoException("Ya existe otro espacio llamado '" + r.nombre() + "'");
        }
        if (r.capacidad() < e.getCapacidad()) {
            validarReduccionCapacidad(e, r.capacidad());
        }
        e.setNombre(r.nombre());
        e.setTipo(r.tipo());
        e.setCapacidad(r.capacidad());
        e.setPrecioHora(r.precioHora());
        e.setEquipamiento(r.equipamiento());
        e.setUbicacion(r.ubicacion());
        return EspacioResponse.de(repo.save(e));
    }

    @Transactional
    public void desactivar(Long id) {
        Espacio e = buscar(id);
        if (!e.isActivo()) {
            throw new ConflictoEstadoException("El espacio ya esta desactivado");
        }
        long futuras = agenda.actividadesFuturas(id, ahora());
        if (futuras > 0) {
            throw new ConflictoEstadoException("El espacio '" + e.getNombre() + "' tiene " + futuras
                    + " actividad(es) programada(s) a futuro. Reprogramalas o cancelalas antes de desactivarlo.");
        }
        e.setActivo(false);
        repo.save(e);
    }

    @Transactional
    public void activar(Long id) {
        Espacio e = buscar(id);
        if (e.isActivo()) {
            throw new ConflictoEstadoException("El espacio ya esta activo");
        }
        e.setActivo(true);
        repo.save(e);
    }

    /** RF09 Consultar disponibilidad de espacios en un rango, con costo estimado del alquiler. */
    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> disponibilidad(DisponibilidadFiltro f) {
        if (f.inicio() == null || f.fin() == null) {
            throw new ReglaNegocioException("Indica inicio y fin (AAAA-MM-DDTHH:mm)");
        }
        if (!f.fin().isAfter(f.inicio())) {
            throw new ReglaNegocioException("El fin debe ser posterior al inicio");
        }
        if (Duration.between(f.inicio(), f.fin()).toDays() > 31) {
            throw new ReglaNegocioException("El rango maximo de consulta es de 31 dias");
        }
        EspacioFiltro ef = new EspacioFiltro(null, f.tipo(), true, f.capacidadMinima());
        return repo.findAll(spec(ef), Sort.by("nombre")).stream().map(e -> {
            var conflicto = agenda.conflicto(e.getId(), f.inicio(), f.fin(), f.excluirTipo(), f.excluirReferenciaId());
            return new DisponibilidadResponse(e.getId(), e.getNombre(), e.getTipo(), e.getCapacidad(), e.getPrecioHora(),
                    e.getPrecioHora().multiply(Horarios.horas(f.inicio(), f.fin())), conflicto.isEmpty(),
                    conflicto.map(a -> a.getTitulo() + " (" + AgendaService.FMT.format(a.getInicio()) + " - "
                            + AgendaService.FMT.format(a.getFin()) + ")").orElse(null));
        }).toList();
    }

    public Espacio buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", id));
    }

    private void validarReduccionCapacidad(Espacio e, int nueva) {
        LocalDateTime ahora = ahora();
        int cupoMax = Stream.concat(cursoRepository.findByEspacioIdAndActivoTrueAndFinAfter(e.getId(), ahora).stream(),
                        diplomadoRepository.findByEspacioIdAndActivoTrueAndFinAfter(e.getId(), ahora).stream())
                .map(OfertaAcademica::getCupoMaximo).max(Integer::compareTo).orElse(0);
        if (cupoMax > nueva) {
            throw new ConflictoEstadoException("La capacidad no puede bajar a " + nueva + ": hay una oferta vigente con "
                    + "cupo de " + cupoMax + " en este espacio");
        }
        if (reservaRepository.existsByEspacioIdAndEstadoInAndFinAfterAndNumeroAsistentesGreaterThan(e.getId(),
                EstadoReserva.VIGENTES, ahora, nueva)
                || cateringRepository.existsByEspacioIdAndEstadoInAndFinAfterAndNumeroAsistentesGreaterThan(e.getId(),
                EstadoSolicitudCatering.VIGENTES, ahora, nueva)) {
            throw new ConflictoEstadoException("La capacidad no puede bajar a " + nueva + ": hay reservas o catering "
                    + "vigentes con mas asistentes");
        }
    }

    private Specification<Espacio> spec(EspacioFiltro f) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    String like = "%" + texto.toLowerCase() + "%";
                    p.add(cb.or(cb.like(cb.lower(root.get("nombre")), like),
                            cb.like(cb.lower(cb.coalesce(root.<String>get("equipamiento"), "")), like),
                            cb.like(cb.lower(cb.coalesce(root.<String>get("ubicacion"), "")), like)));
                }
                if (f.tipo() != null) {
                    p.add(cb.equal(root.get("tipo"), f.tipo()));
                }
                if (f.activo() != null) {
                    p.add(cb.equal(root.get("activo"), f.activo()));
                }
                if (f.capacidadMinima() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("capacidad"), f.capacidadMinima()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
