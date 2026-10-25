package edu.udb.ucacfc.reporte;

import edu.udb.ucacfc.academico.docente.DocenteRepository;
import edu.udb.ucacfc.academico.oferta.CursoRepository;
import edu.udb.ucacfc.academico.oferta.DiplomadoRepository;
import edu.udb.ucacfc.catering.EstadoSolicitudCatering;
import edu.udb.ucacfc.catering.SolicitudCateringRepository;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.cotizacion.CotizacionRepository;
import edu.udb.ucacfc.cotizacion.EstadoCotizacion;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacioRepository;
import edu.udb.ucacfc.inscripcion.EstadoInscripcion;
import edu.udb.ucacfc.inscripcion.InscripcionRepository;
import edu.udb.ucacfc.pago.Abono;
import edu.udb.ucacfc.pago.AbonoRepository;
import edu.udb.ucacfc.pago.EstadoAbono;
import edu.udb.ucacfc.pago.EstadoPago;
import edu.udb.ucacfc.pago.MetodoPago;
import edu.udb.ucacfc.pago.Pago;
import edu.udb.ucacfc.pago.PagoRepository;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import edu.udb.ucacfc.reporte.dto.FinancieroResponse;
import edu.udb.ucacfc.reporte.dto.ResumenResponse;
import edu.udb.ucacfc.reporte.dto.ResumenResponse.OfertaTop;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reportes de solo lectura: resumen operativo (ADMIN) y estado financiero (ADMIN, CONTABILIDAD). */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ClienteRepository clienteRepository;
    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final EspacioRepository espacioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ReservaEspacioRepository reservaRepository;
    private final SolicitudCateringRepository cateringRepository;
    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;
    private final AbonoRepository abonoRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ResumenResponse resumen() {
        Map<String, Long> ins = new LinkedHashMap<>();
        for (EstadoInscripcion e : EstadoInscripcion.values()) {
            ins.put(e.name(), inscripcionRepository.countByEstado(e));
        }
        Map<String, Long> res = new LinkedHashMap<>();
        for (EstadoReserva e : EstadoReserva.values()) {
            res.put(e.name(), reservaRepository.countByEstado(e));
        }
        Map<String, Long> cat = new LinkedHashMap<>();
        for (EstadoSolicitudCatering e : EstadoSolicitudCatering.values()) {
            cat.put(e.name(), cateringRepository.countByEstado(e));
        }
        Map<String, Long> cot = new LinkedHashMap<>();
        for (EstadoCotizacion e : EstadoCotizacion.values()) {
            cot.put(e.name(), cotizacionRepository.countByEstado(e));
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<OfertaTop> top = new ArrayList<>();
        cursoRepository.findAll(vigentes(ahora)).forEach(c -> top.add(new OfertaTop("CURSO", c.getId(), c.getNombre(),
                inscripcionRepository.countByCursoIdAndEstadoIn(c.getId(), EstadoInscripcion.VIGENTES), c.getCupoMaximo())));
        diplomadoRepository.findAll(vigentes(ahora)).forEach(d -> top.add(new OfertaTop("DIPLOMADO", d.getId(),
                d.getNombre(), inscripcionRepository.countByDiplomadoIdAndEstadoIn(d.getId(), EstadoInscripcion.VIGENTES),
                d.getCupoMaximo())));
        top.sort(Comparator.comparingLong(OfertaTop::inscritos).reversed());

        return new ResumenResponse(
                clienteRepository.count((r, q, cb) -> cb.isTrue(r.get("activo"))),
                docenteRepository.count((r, q, cb) -> cb.isTrue(r.get("activo"))),
                cursoRepository.countByActivoTrue(), diplomadoRepository.countByActivoTrue(),
                espacioRepository.count((r, q, cb) -> cb.isTrue(r.get("activo"))),
                ins, res, cat, cot, top.size() > 5 ? top.subList(0, 5) : top);
    }

    @Transactional(readOnly = true)
    public FinancieroResponse financiero(LocalDate desde, LocalDate hasta) {
        LocalDate h = hasta == null ? LocalDate.now(clock) : hasta;
        LocalDate d = desde == null ? h.withDayOfMonth(1) : desde;
        if (h.isBefore(d)) {
            throw new ReglaNegocioException("'hasta' no puede ser anterior a 'desde'");
        }
        LocalDateTime ini = d.atStartOfDay();
        LocalDateTime fin = h.plusDays(1).atStartOfDay();

        List<Pago> generadas = pagoRepository.findAll((r, q, cb) -> cb.and(cb.notEqual(r.get("estado"), EstadoPago.ANULADO),
                cb.greaterThanOrEqualTo(r.get("fechaCreacion"), ini), cb.lessThan(r.get("fechaCreacion"), fin)));
        List<Abono> validados = abonoRepository.findAll((r, q, cb) -> cb.and(cb.equal(r.get("estado"), EstadoAbono.VALIDADO),
                cb.greaterThanOrEqualTo(r.get("fechaValidacion"), ini), cb.lessThan(r.get("fechaValidacion"), fin)));
        List<Pago> abiertas = pagoRepository.findAll((r, q, cb) -> r.get("estado").in(EstadoPago.PENDIENTE, EstadoPago.PARCIAL));
        List<Abono> enValidacion = abonoRepository.findAll((r, q, cb) -> cb.equal(r.get("estado"), EstadoAbono.PENDIENTE_VALIDACION));

        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        for (MetodoPago m : MetodoPago.values()) {
            porMetodo.put(m.name(), validados.stream().filter(a -> a.getMetodo() == m).map(Abono::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        Map<String, BigDecimal> porTipo = new LinkedHashMap<>();
        for (TipoReferenciaPago t : TipoReferenciaPago.values()) {
            porTipo.put(t.name(), generadas.stream().filter(p -> p.getTipoReferencia() == t).map(Pago::getMontoTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoPago e : EstadoPago.values()) {
            porEstado.put(e.name(), pagoRepository.count((r, q, cb) -> cb.equal(r.get("estado"), e)));
        }
        return new FinancieroResponse(d, h,
                generadas.stream().map(Pago::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                validados.stream().map(Abono::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add),
                abiertas.stream().map(Pago::saldo).reduce(BigDecimal.ZERO, BigDecimal::add),
                enValidacion.stream().map(Abono::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add),
                enValidacion.size(), porMetodo, porTipo, porEstado);
    }

    private static <T> Specification<T> vigentes(LocalDateTime ahora) {
        return (r, q, cb) -> cb.and(cb.isTrue(r.get("activo")), cb.greaterThan(r.get("fin"), ahora));
    }
}
