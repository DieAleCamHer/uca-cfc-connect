package edu.udb.ucacfc.espacio.reserva;

import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.agenda.NuevaActividad;
import edu.udb.ucacfc.agenda.TipoActividad;
import edu.udb.ucacfc.catering.EstadoSolicitudCatering;
import edu.udb.ucacfc.catering.SolicitudCatering;
import edu.udb.ucacfc.catering.SolicitudCateringRepository;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteService;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioService;
import edu.udb.ucacfc.espacio.reserva.dto.ReservaFiltro;
import edu.udb.ucacfc.espacio.reserva.dto.ReservaRequest;
import edu.udb.ucacfc.espacio.reserva.dto.ReservaResponse;
import edu.udb.ucacfc.pago.Pago;
import edu.udb.ucacfc.pago.PagoService;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.config.ReglasNegocioProperties;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.security.AccesoService;
import edu.udb.ucacfc.shared.util.Horarios;
import edu.udb.ucacfc.shared.util.MaquinaEstados;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import edu.udb.ucacfc.shared.web.PaginacionUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static edu.udb.ucacfc.espacio.reserva.EstadoReserva.CANCELADA;
import static edu.udb.ucacfc.espacio.reserva.EstadoReserva.COMPLETADA;
import static edu.udb.ucacfc.espacio.reserva.EstadoReserva.CONFIRMADA;
import static edu.udb.ucacfc.espacio.reserva.EstadoReserva.RECHAZADA;
import static edu.udb.ucacfc.espacio.reserva.EstadoReserva.SOLICITADA;

/**
 * Modulo 5 - Alquiler de espacios (RF07). Reglas:
 *  - Espacio y cliente activos; la reserva inicia en el futuro; dura entre 30 min y 12 h.
 *  - Asistentes <= capacidad del espacio.
 *  - Sin conflictos de horario: la AGENDA valida contra cursos, diplomados, eventos, otras reservas y catering.
 *  - Costo = horas x precio por hora del espacio, congelado en la reserva (se recalcula si se edita).
 *  - Confirmar genera la cuenta; completar exige horario terminado y cuenta PAGADA (o lo hace el cierre automatico).
 *  - Cancelar: el CLIENTE hasta N horas antes (app.reglas.cancelacion-cliente-horas); el personal hasta el inicio.
 *    Nunca con dinero cobrado, ni con un catering vigente dentro de la reserva (cancelar ese catering primero).
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final Map<String, String> SORT = Map.of("id", "id", "inicio", "inicio", "fin", "fin",
            "estado", "estado", "total", "total", "espacio", "espacio.nombre", "cliente", "cliente.nombre");
    private static final MaquinaEstados<EstadoReserva> MAQUINA = MaquinaEstados.de("reserva", EstadoReserva.class)
            .permitir(SOLICITADA, CONFIRMADA, RECHAZADA, CANCELADA)
            .permitir(CONFIRMADA, COMPLETADA, CANCELADA);
    private static final Duration MIN = Duration.ofMinutes(30);
    private static final Duration MAX = Duration.ofHours(12);

    private final ReservaEspacioRepository repo;
    private final EspacioService espacioService;
    private final ClienteService clienteService;
    private final AgendaService agenda;
    private final PagoService pagoService;
    private final SolicitudCateringRepository cateringRepository;
    private final AccesoService acceso;
    private final ReglasNegocioProperties reglas;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<ReservaResponse> listar(ReservaFiltro f, PaginaRequest pagina, Usuario usuario) {
        Long clienteId = acceso.filtroCliente(usuario, f == null ? null : f.clienteId());
        Specification<ReservaEspacio> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (clienteId != null) {
                p.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (f != null) {
                if (f.espacioId() != null) {
                    p.add(cb.equal(root.get("espacio").get("id"), f.espacioId()));
                }
                if (f.estado() != null) {
                    p.add(cb.equal(root.get("estado"), f.estado()));
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
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "inicio,desc"))
                .map(r -> aResponse(r, usuario)));
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id, Usuario usuario) {
        ReservaEspacio r = buscar(id);
        acceso.verificarDueno(usuario, r.getCliente().getId(), "una reserva");
        return aResponse(r, usuario);
    }

    @Transactional
    public ReservaResponse crear(ReservaRequest req, Usuario usuario) {
        Cliente cliente = clienteService.buscarActivo(acceso.resolverClienteParaCrear(usuario, req.clienteId()));
        Espacio espacio = validar(req);
        ReservaEspacio r = ReservaEspacio.builder().cliente(cliente).estado(SOLICITADA).build();
        copiar(r, req, espacio);
        r = repo.save(r);
        registrarAgenda(r);
        return aResponse(r, usuario);
    }

    /** Solo mientras esta SOLICITADA: cambia horario, espacio, asistentes o proposito y revalida todo. */
    @Transactional
    public ReservaResponse actualizar(Long id, ReservaRequest req, Usuario usuario) {
        ReservaEspacio r = buscar(id);
        acceso.verificarDueno(usuario, r.getCliente().getId(), "una reserva");
        if (r.getEstado() != SOLICITADA) {
            throw new ConflictoEstadoException("Solo se edita una reserva SOLICITADA; esta esta " + r.getEstado());
        }
        if (!cateringRepository.findByReservaIdAndEstadoIn(id, EstadoSolicitudCatering.VIGENTES).isEmpty()) {
            throw new ConflictoEstadoException("La reserva tiene catering vigente asociado: cancelalo antes de cambiar la reserva");
        }
        Espacio espacio = validar(req);
        agenda.eliminar(TipoActividad.ALQUILER, id);
        copiar(r, req, espacio);
        r = repo.save(r);
        registrarAgenda(r);
        return aResponse(r, usuario);
    }

    @Transactional
    public ReservaResponse confirmar(Long id, Usuario usuario) {
        ReservaEspacio r = buscar(id);
        MAQUINA.validar(r.getEstado(), CONFIRMADA);
        if (!r.getInicio().isAfter(ahora())) {
            throw new ConflictoEstadoException("La reserva ya inicio: no se puede confirmar");
        }
        r.setEstado(CONFIRMADA);
        repo.save(r);
        pagoService.generarCuenta(TipoReferenciaPago.ALQUILER, id, r.getCliente(),
                "Alquiler #" + id + " - " + r.getEspacio().getNombre() + " " + AgendaService.FMT.format(r.getInicio())
                        + " (" + r.getHoras() + " h)", r.getTotal());
        return aResponse(r, usuario);
    }

    @Transactional
    public ReservaResponse rechazar(Long id, String motivo, Usuario usuario) {
        ReservaEspacio r = buscar(id);
        MAQUINA.validar(r.getEstado(), RECHAZADA);
        verificarSinCatering(id);
        r.setEstado(RECHAZADA);
        r.setMotivoEstado(motivo);
        agenda.eliminar(TipoActividad.ALQUILER, id);
        return aResponse(repo.save(r), usuario);
    }

    @Transactional
    public ReservaResponse cancelar(Long id, String motivo, Usuario usuario) {
        ReservaEspacio r = buscar(id);
        acceso.verificarDueno(usuario, r.getCliente().getId(), "una reserva");
        MAQUINA.validar(r.getEstado(), CANCELADA);
        LocalDateTime ahora = ahora();
        if (!r.getInicio().isAfter(ahora)) {
            throw new ConflictoEstadoException("La reserva ya inicio: no se puede cancelar");
        }
        if (acceso.esCliente(usuario) && r.getInicio().isBefore(ahora.plusHours(reglas.cancelacionClienteHoras()))) {
            throw new ConflictoEstadoException("Solo puedes cancelar hasta " + reglas.cancelacionClienteHoras()
                    + " horas antes del inicio. Comunicate con recepcion.");
        }
        verificarSinCatering(id);
        pagoService.anularCuenta(TipoReferenciaPago.ALQUILER, id, "Reserva cancelada");
        r.setEstado(CANCELADA);
        r.setMotivoEstado(motivo == null || motivo.isBlank()
                ? (acceso.esCliente(usuario) ? "Cancelada por el cliente" : "Cancelada por el CFC") : motivo);
        agenda.eliminar(TipoActividad.ALQUILER, id);
        return aResponse(repo.save(r), usuario);
    }

    @Transactional
    public ReservaResponse completar(Long id, Usuario usuario) {
        ReservaEspacio r = buscar(id);
        MAQUINA.validar(r.getEstado(), COMPLETADA);
        if (ahora().isBefore(r.getFin())) {
            throw new ConflictoEstadoException("La reserva termina el " + AgendaService.FMT.format(r.getFin())
                    + ": aun no se puede completar");
        }
        pagoService.exigirSaldado(TipoReferenciaPago.ALQUILER, id);
        r.setEstado(COMPLETADA);
        return aResponse(repo.save(r), usuario);
    }

    public ReservaEspacio buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Reserva", id));
    }

    // ------------------------------------------------------------------------------------------------

    private Espacio validar(ReservaRequest req) {
        Espacio e = espacioService.buscar(req.espacioId());
        if (!e.isActivo()) {
            throw new ReglaNegocioException("El espacio '" + e.getNombre() + "' esta desactivado");
        }
        if (!req.inicio().isAfter(ahora())) {
            throw new ReglaNegocioException("La reserva debe iniciar en el futuro");
        }
        if (!req.fin().isAfter(req.inicio())) {
            throw new ReglaNegocioException("El fin debe ser posterior al inicio");
        }
        Duration d = Duration.between(req.inicio(), req.fin());
        if (d.compareTo(MIN) < 0 || d.compareTo(MAX) > 0) {
            throw new ReglaNegocioException("Una reserva dura entre 30 minutos y 12 horas");
        }
        if (req.numeroAsistentes() > e.getCapacidad()) {
            throw new ReglaNegocioException("El espacio '" + e.getNombre() + "' tiene capacidad para "
                    + e.getCapacidad() + " personas (solicitaste " + req.numeroAsistentes() + ")");
        }
        return e;
    }

    private void copiar(ReservaEspacio r, ReservaRequest req, Espacio e) {
        LocalDateTime inicio = req.inicio().withSecond(0).withNano(0);
        LocalDateTime fin = req.fin().withSecond(0).withNano(0);
        BigDecimal horas = Horarios.horas(inicio, fin);
        r.setEspacio(e);
        r.setInicio(inicio);
        r.setFin(fin);
        r.setNumeroAsistentes(req.numeroAsistentes());
        r.setProposito(req.proposito());
        r.setHoras(horas);
        r.setPrecioHora(e.getPrecioHora());
        r.setTotal(e.getPrecioHora().multiply(horas).setScale(2, RoundingMode.HALF_UP));
    }

    private void registrarAgenda(ReservaEspacio r) {
        agenda.registrar(new NuevaActividad(TipoActividad.ALQUILER, r.getId(),
                "Alquiler: " + r.getEspacio().getNombre() + " - " + r.getCliente().nombreMostrado(),
                r.getInicio(), r.getFin(), r.getEspacio(), true, r.getCliente().getId(), r.getProposito()));
    }

    private void verificarSinCatering(Long reservaId) {
        List<SolicitudCatering> vigentes = cateringRepository.findByReservaIdAndEstadoIn(reservaId,
                EstadoSolicitudCatering.VIGENTES);
        if (!vigentes.isEmpty()) {
            throw new ConflictoEstadoException("La reserva tiene la solicitud de catering #" + vigentes.get(0).getId()
                    + " vigente: cancelala primero");
        }
    }

    private Set<EstadoReserva> acciones(ReservaEspacio r, Usuario u) {
        Set<EstadoReserva> s = EnumSet.noneOf(EstadoReserva.class);
        s.addAll(MAQUINA.siguientes(r.getEstado()));
        LocalDateTime ahora = ahora();
        if (!r.getInicio().isAfter(ahora)) {
            s.remove(CANCELADA);
            s.remove(CONFIRMADA);
        }
        if (ahora.isBefore(r.getFin())) {
            s.remove(COMPLETADA);
        }
        if (u.getRol() == Rol.CLIENTE) {
            s.retainAll(EnumSet.of(CANCELADA));
            if (r.getInicio().isBefore(ahora.plusHours(reglas.cancelacionClienteHoras()))) {
                s.clear();
            }
        } else if (u.getRol() == Rol.CONTABILIDAD) {
            s.clear();
        }
        return s;
    }

    private ReservaResponse aResponse(ReservaEspacio r, Usuario u) {
        Optional<Pago> pago = pagoService.cuentaActiva(TipoReferenciaPago.ALQUILER, r.getId());
        return new ReservaResponse(r.getId(), r.getEspacio().getId(), r.getEspacio().getNombre(),
                r.getCliente().getId(), r.getCliente().nombreMostrado(), r.getInicio(), r.getFin(),
                r.getNumeroAsistentes(), r.getProposito(), r.getHoras(), r.getPrecioHora(), r.getTotal(), r.getEstado(),
                r.getMotivoEstado(), pago.map(Pago::getId).orElse(null), pago.map(Pago::getEstado).orElse(null),
                pago.map(Pago::saldo).orElse(null), acciones(r, u));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
