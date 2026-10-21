package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.agenda.NuevaActividad;
import edu.udb.ucacfc.agenda.TipoActividad;
import edu.udb.ucacfc.catering.dto.SolicitudCateringFiltro;
import edu.udb.ucacfc.catering.dto.SolicitudCateringRequest;
import edu.udb.ucacfc.catering.dto.SolicitudCateringResponse;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteService;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioService;
import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacio;
import edu.udb.ucacfc.espacio.reserva.ReservaService;
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
import edu.udb.ucacfc.shared.util.MaquinaEstados;
import edu.udb.ucacfc.shared.util.Textos;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import edu.udb.ucacfc.shared.web.PaginacionUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static edu.udb.ucacfc.catering.EstadoSolicitudCatering.ATENDIDA;
import static edu.udb.ucacfc.catering.EstadoSolicitudCatering.CANCELADA;
import static edu.udb.ucacfc.catering.EstadoSolicitudCatering.CONFIRMADA;
import static edu.udb.ucacfc.catering.EstadoSolicitudCatering.PENDIENTE;
import static edu.udb.ucacfc.catering.EstadoSolicitudCatering.RECHAZADA;

/**
 * Modulo 6 - Catering (RF08). El SERVICE calcula el costo (lineamiento: "calcular costos de catering"):
 * total = precio por persona del servicio x asistentes, congelado en la solicitud.
 * Reglas:
 *  - Servicio y cliente activos. Se pide con al menos N horas de anticipacion (app.reglas.catering-anticipacion-horas).
 *  - Mismo dia, horaFin > horaInicio, maximo 8 horas.
 *  - Lugar: reserva del mismo cliente (vigente y que cubra el horario) | espacio libre del CFC | lugar externo.
 *    Con espacio o reserva: asistentes <= capacidad.
 *  - Confirmar genera la cuenta. Atender exige que el evento haya iniciado y la cuenta este PAGADA.
 *  - Cancelar: CLIENTE hasta N horas antes; personal hasta el inicio; nunca con dinero cobrado.
 */
@Service
@RequiredArgsConstructor
public class SolicitudCateringService {

    private static final Map<String, String> SORT = Map.of("id", "id", "inicio", "inicio", "estado", "estado",
            "total", "total", "numeroAsistentes", "numeroAsistentes", "cliente", "cliente.nombre",
            "servicio", "servicio.nombre");
    private static final MaquinaEstados<EstadoSolicitudCatering> MAQUINA =
            MaquinaEstados.de("solicitud de catering", EstadoSolicitudCatering.class)
                    .permitir(PENDIENTE, CONFIRMADA, RECHAZADA, CANCELADA)
                    .permitir(CONFIRMADA, ATENDIDA, CANCELADA);
    private static final Duration MAX = Duration.ofHours(8);

    private final SolicitudCateringRepository repo;
    private final ServicioCateringService servicioService;
    private final ClienteService clienteService;
    private final EspacioService espacioService;
    private final ReservaService reservaService;
    private final AgendaService agenda;
    private final PagoService pagoService;
    private final AccesoService acceso;
    private final ReglasNegocioProperties reglas;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<SolicitudCateringResponse> listar(SolicitudCateringFiltro f, PaginaRequest pagina, Usuario usuario) {
        Long clienteId = acceso.filtroCliente(usuario, f == null ? null : f.clienteId());
        Specification<SolicitudCatering> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (clienteId != null) {
                p.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (f != null) {
                if (f.servicioId() != null) {
                    p.add(cb.equal(root.get("servicio").get("id"), f.servicioId()));
                }
                if (f.estado() != null) {
                    p.add(cb.equal(root.get("estado"), f.estado()));
                }
                if (f.desde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("inicio"), f.desde().atStartOfDay()));
                }
                if (f.hasta() != null) {
                    p.add(cb.lessThan(root.get("inicio"), f.hasta().plusDays(1).atStartOfDay()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "inicio,desc"))
                .map(s -> aResponse(s, usuario)));
    }

    @Transactional(readOnly = true)
    public SolicitudCateringResponse obtener(Long id, Usuario usuario) {
        SolicitudCatering s = buscar(id);
        acceso.verificarDueno(usuario, s.getCliente().getId(), "una solicitud de catering");
        return aResponse(s, usuario);
    }

    @Transactional
    public SolicitudCateringResponse crear(SolicitudCateringRequest r, Usuario usuario) {
        Cliente cliente = clienteService.buscarActivo(acceso.resolverClienteParaCrear(usuario, r.clienteId()));
        SolicitudCatering s = SolicitudCatering.builder().cliente(cliente).estado(PENDIENTE).build();
        aplicar(s, r);
        s = repo.save(s);
        registrarAgenda(s);
        return aResponse(s, usuario);
    }

    @Transactional
    public SolicitudCateringResponse actualizar(Long id, SolicitudCateringRequest r, Usuario usuario) {
        SolicitudCatering s = buscar(id);
        acceso.verificarDueno(usuario, s.getCliente().getId(), "una solicitud de catering");
        if (s.getEstado() != PENDIENTE) {
            throw new ConflictoEstadoException("Solo se edita una solicitud PENDIENTE; esta esta " + s.getEstado());
        }
        agenda.eliminar(TipoActividad.CATERING, id);
        aplicar(s, r);
        s = repo.save(s);
        registrarAgenda(s);
        return aResponse(s, usuario);
    }

    @Transactional
    public SolicitudCateringResponse confirmar(Long id, Usuario usuario) {
        SolicitudCatering s = buscar(id);
        MAQUINA.validar(s.getEstado(), CONFIRMADA);
        if (!s.getInicio().isAfter(ahora())) {
            throw new ConflictoEstadoException("El evento ya inicio: no se puede confirmar");
        }
        if (!s.getServicio().isActivo()) {
            throw new ConflictoEstadoException("El servicio '" + s.getServicio().getNombre() + "' fue desactivado");
        }
        s.setEstado(CONFIRMADA);
        repo.save(s);
        pagoService.generarCuenta(TipoReferenciaPago.CATERING, id, s.getCliente(),
                "Catering #" + id + " - " + s.getServicio().getNombre() + " x " + s.getNumeroAsistentes() + " ("
                        + AgendaService.FMT.format(s.getInicio()) + ")", s.getTotal());
        return aResponse(s, usuario);
    }

    @Transactional
    public SolicitudCateringResponse rechazar(Long id, String motivo, Usuario usuario) {
        SolicitudCatering s = buscar(id);
        MAQUINA.validar(s.getEstado(), RECHAZADA);
        s.setEstado(RECHAZADA);
        s.setMotivoEstado(motivo);
        agenda.eliminar(TipoActividad.CATERING, id);
        return aResponse(repo.save(s), usuario);
    }

    @Transactional
    public SolicitudCateringResponse cancelar(Long id, String motivo, Usuario usuario) {
        SolicitudCatering s = buscar(id);
        acceso.verificarDueno(usuario, s.getCliente().getId(), "una solicitud de catering");
        MAQUINA.validar(s.getEstado(), CANCELADA);
        LocalDateTime ahora = ahora();
        if (!s.getInicio().isAfter(ahora)) {
            throw new ConflictoEstadoException("El evento ya inicio: no se puede cancelar");
        }
        if (acceso.esCliente(usuario) && s.getInicio().isBefore(ahora.plusHours(reglas.cancelacionClienteHoras()))) {
            throw new ConflictoEstadoException("Solo puedes cancelar hasta " + reglas.cancelacionClienteHoras()
                    + " horas antes: el pedido ya esta en preparacion. Comunicate con recepcion.");
        }
        pagoService.anularCuenta(TipoReferenciaPago.CATERING, id, "Catering cancelado");
        s.setEstado(CANCELADA);
        s.setMotivoEstado(motivo == null || motivo.isBlank()
                ? (acceso.esCliente(usuario) ? "Cancelada por el cliente" : "Cancelada por el CFC") : motivo);
        agenda.eliminar(TipoActividad.CATERING, id);
        return aResponse(repo.save(s), usuario);
    }

    @Transactional
    public SolicitudCateringResponse atender(Long id, Usuario usuario) {
        SolicitudCatering s = buscar(id);
        MAQUINA.validar(s.getEstado(), ATENDIDA);
        if (ahora().isBefore(s.getInicio())) {
            throw new ConflictoEstadoException("No se puede marcar ATENDIDA antes del evento ("
                    + AgendaService.FMT.format(s.getInicio()) + ")");
        }
        pagoService.exigirSaldado(TipoReferenciaPago.CATERING, id);
        s.setEstado(ATENDIDA);
        return aResponse(repo.save(s), usuario);
    }

    // ------------------------------------------------------------------------------------------------

    private void aplicar(SolicitudCatering s, SolicitudCateringRequest r) {
        ServicioCatering servicio = servicioService.buscar(r.servicioId());
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio '" + servicio.getNombre() + "' no esta disponible");
        }
        LocalDateTime inicio = r.fecha().atTime(r.horaInicio().withSecond(0).withNano(0));
        LocalDateTime fin = r.fecha().atTime(r.horaFin().withSecond(0).withNano(0));
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (Duration.between(inicio, fin).compareTo(MAX) > 0) {
            throw new ReglaNegocioException("Un servicio de catering dura como maximo 8 horas");
        }
        LocalDateTime limite = ahora().plusHours(reglas.cateringAnticipacionHoras());
        if (inicio.isBefore(limite)) {
            throw new ReglaNegocioException("El catering se solicita con al menos " + reglas.cateringAnticipacionHoras()
                    + " horas de anticipacion (a partir del " + AgendaService.FMT.format(limite) + ")");
        }
        if (r.reservaId() != null && r.espacioId() != null) {
            throw new ReglaNegocioException("Indica la reserva O el espacio, no ambos (la reserva ya define el espacio)");
        }
        Espacio espacio = null;
        ReservaEspacio reserva = null;
        String lugar = Textos.vacioANull(r.lugar());
        if (r.reservaId() != null) {
            reserva = reservaService.buscar(r.reservaId());
            if (!reserva.getCliente().getId().equals(s.getCliente().getId())) {
                throw new ReglaNegocioException("La reserva #" + reserva.getId() + " pertenece a otro cliente");
            }
            if (!EstadoReserva.VIGENTES.contains(reserva.getEstado())) {
                throw new ReglaNegocioException("La reserva #" + reserva.getId() + " no esta vigente (" + reserva.getEstado() + ")");
            }
            if (inicio.isBefore(reserva.getInicio()) || fin.isAfter(reserva.getFin())) {
                throw new ReglaNegocioException("El catering debe servirse dentro del horario de la reserva ("
                        + AgendaService.FMT.format(reserva.getInicio()) + " - " + AgendaService.FMT.format(reserva.getFin()) + ")");
            }
            espacio = reserva.getEspacio();
        } else if (r.espacioId() != null) {
            espacio = espacioService.buscar(r.espacioId());
            if (!espacio.isActivo()) {
                throw new ReglaNegocioException("El espacio '" + espacio.getNombre() + "' esta desactivado");
            }
        } else if (lugar == null) {
            throw new ReglaNegocioException("Indica el lugar del evento (o una reserva / espacio del CFC)");
        }
        if (espacio != null) {
            if (r.numeroAsistentes() > espacio.getCapacidad()) {
                throw new ReglaNegocioException("El espacio '" + espacio.getNombre() + "' tiene capacidad para "
                        + espacio.getCapacidad() + " personas");
            }
            if (lugar == null) {
                lugar = espacio.getNombre();
            }
        }
        s.setServicio(servicio);
        s.setNumeroAsistentes(r.numeroAsistentes());
        s.setMenu(r.menu());
        s.setInicio(inicio);
        s.setFin(fin);
        s.setLugar(lugar);
        s.setEspacio(espacio);
        s.setReserva(reserva);
        s.setPrecioUnitario(servicio.getPrecioUnitario());
        s.setTotal(servicio.getPrecioUnitario().multiply(BigDecimal.valueOf(r.numeroAsistentes())));
    }

    /** Catering en espacio libre: bloquea. Dentro de una reserva: visible en agenda pero no bloquea (ya lo hace la reserva). */
    private void registrarAgenda(SolicitudCatering s) {
        if (s.getEspacio() == null) {
            return;
        }
        boolean bloquea = s.getReserva() == null;
        agenda.registrar(new NuevaActividad(TipoActividad.CATERING, s.getId(),
                "Catering: " + s.getServicio().getNombre() + " - " + s.getCliente().nombreMostrado(),
                s.getInicio(), s.getFin(), s.getEspacio(), bloquea, s.getCliente().getId(),
                s.getNumeroAsistentes() + " asistentes" + (bloquea ? "" : " (dentro de la reserva #" + s.getReserva().getId() + ")")));
    }

    private SolicitudCatering buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Solicitud de catering", id));
    }

    private Set<EstadoSolicitudCatering> acciones(SolicitudCatering s, Usuario u) {
        Set<EstadoSolicitudCatering> a = EnumSet.noneOf(EstadoSolicitudCatering.class);
        a.addAll(MAQUINA.siguientes(s.getEstado()));
        LocalDateTime ahora = ahora();
        if (!s.getInicio().isAfter(ahora)) {
            a.remove(CANCELADA);
            a.remove(CONFIRMADA);
        } else {
            a.remove(ATENDIDA);
        }
        if (u.getRol() == Rol.CLIENTE) {
            a.retainAll(EnumSet.of(CANCELADA));
            if (s.getInicio().isBefore(ahora.plusHours(reglas.cancelacionClienteHoras()))) {
                a.clear();
            }
        } else if (u.getRol() == Rol.CONTABILIDAD) {
            a.clear();
        }
        return a;
    }

    private SolicitudCateringResponse aResponse(SolicitudCatering s, Usuario u) {
        Optional<Pago> pago = pagoService.cuentaActiva(TipoReferenciaPago.CATERING, s.getId());
        return new SolicitudCateringResponse(s.getId(), s.getCliente().getId(), s.getCliente().nombreMostrado(),
                s.getServicio().getId(), s.getServicio().getNombre(), s.getServicio().getTipo(), s.getNumeroAsistentes(),
                s.getMenu(), s.getInicio(), s.getFin(), s.getLugar(),
                s.getEspacio() == null ? null : s.getEspacio().getId(),
                s.getEspacio() == null ? null : s.getEspacio().getNombre(),
                s.getReserva() == null ? null : s.getReserva().getId(),
                s.getPrecioUnitario(), s.getTotal(), s.getEstado(), s.getMotivoEstado(),
                pago.map(Pago::getId).orElse(null), pago.map(Pago::getEstado).orElse(null),
                pago.map(Pago::saldo).orElse(null), acciones(s, u));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
