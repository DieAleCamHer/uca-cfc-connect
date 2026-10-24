package edu.udb.ucacfc.cierre;

import edu.udb.ucacfc.agenda.ActividadAgendaRepository;
import edu.udb.ucacfc.agenda.TipoActividad;
import edu.udb.ucacfc.catering.EstadoSolicitudCatering;
import edu.udb.ucacfc.catering.SolicitudCatering;
import edu.udb.ucacfc.catering.SolicitudCateringRepository;
import edu.udb.ucacfc.cotizacion.Cotizacion;
import edu.udb.ucacfc.cotizacion.CotizacionRepository;
import edu.udb.ucacfc.cotizacion.EstadoCotizacion;
import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacio;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacioRepository;
import edu.udb.ucacfc.inscripcion.EstadoInscripcion;
import edu.udb.ucacfc.inscripcion.Inscripcion;
import edu.udb.ucacfc.inscripcion.InscripcionRepository;
import edu.udb.ucacfc.pago.EstadoPago;
import edu.udb.ucacfc.pago.PagoRepository;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Cierre y vencimiento automaticos. Dos disparadores:
 *  1) alSaldarse(): el abono que deja una cuenta PAGADA intenta cerrar su proceso al instante (si ya termino).
 *  2) barrido(): cada 15 min (CierreAutomaticoTarea) cierra lo terminado y pagado, y vence lo que nadie atendio:
 *     - Inscripcion CONFIRMADA + oferta terminada + PAGADA       -> FINALIZADA
 *     - Reserva / catering CONFIRMADO + horario terminado + PAGADO -> COMPLETADA / ATENDIDA
 *     - Inscripcion PENDIENTE con la oferta ya terminada          -> CANCELADA (nunca se confirmo)
 *     - Reserva SOLICITADA / catering PENDIENTE cuya hora ya llego -> CANCELADA y se libera el espacio
 *     - Cotizacion PENDIENTE / EN_PROCESO vencida                 -> RECHAZADA ("Vencida")
 * Usa repositorios directamente (no services) para no crear dependencias circulares con PagoService.
 * Los vencidos nunca tienen cuenta por cobrar (solo nace al confirmar), asi que no hay dinero que proteger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CierreAutomaticoService {

    private final InscripcionRepository inscripcionRepository;
    private final ReservaEspacioRepository reservaRepository;
    private final SolicitudCateringRepository cateringRepository;
    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;
    private final ActividadAgendaRepository agendaRepository;
    private final Clock clock;

    @Transactional
    public void alSaldarse(TipoReferenciaPago tipo, Long referenciaId) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        switch (tipo) {
            case INSCRIPCION -> inscripcionRepository.findById(referenciaId).ifPresent(i -> cerrarInscripcion(i, ahora));
            case ALQUILER -> reservaRepository.findById(referenciaId).ifPresent(r -> cerrarReserva(r, ahora));
            case CATERING -> cateringRepository.findById(referenciaId).ifPresent(c -> cerrarCatering(c, ahora));
            case COTIZACION -> {
                // Una cotizacion aprobada y pagada no cambia de estado: lo contratado se ejecuta en sus modulos.
            }
        }
    }

    /** true si el proceso ya esta en su estado final de exito (su pago ya no se puede revertir). */
    @Transactional(readOnly = true)
    public boolean procesoCerrado(TipoReferenciaPago tipo, Long referenciaId) {
        return switch (tipo) {
            case INSCRIPCION -> inscripcionRepository.findById(referenciaId)
                    .map(i -> i.getEstado() == EstadoInscripcion.FINALIZADA).orElse(false);
            case ALQUILER -> reservaRepository.findById(referenciaId)
                    .map(r -> r.getEstado() == EstadoReserva.COMPLETADA).orElse(false);
            case CATERING -> cateringRepository.findById(referenciaId)
                    .map(c -> c.getEstado() == EstadoSolicitudCatering.ATENDIDA).orElse(false);
            case COTIZACION -> false;
        };
    }

    @Transactional
    public int barrido() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        int n = 0;
        for (Inscripcion i : inscripcionRepository.findByEstado(EstadoInscripcion.CONFIRMADA)) {
            n += cerrarInscripcion(i, ahora) ? 1 : 0;
        }
        for (Inscripcion i : inscripcionRepository.findByEstado(EstadoInscripcion.PENDIENTE)) {
            if (i.getOferta().yaTermino(ahora)) {
                i.setEstado(EstadoInscripcion.CANCELADA);
                i.setFechaCierre(ahora);
                i.setMotivoCancelacion("Cancelada automaticamente: no fue confirmada antes de que terminara la oferta");
                n++;
            }
        }
        for (ReservaEspacio r : reservaRepository.findByEstadoAndFinBefore(EstadoReserva.CONFIRMADA, ahora)) {
            n += cerrarReserva(r, ahora) ? 1 : 0;
        }
        for (ReservaEspacio r : reservaRepository.findByEstadoAndInicioBefore(EstadoReserva.SOLICITADA, ahora)) {
            r.setEstado(EstadoReserva.CANCELADA);
            r.setMotivoEstado("Cancelada automaticamente: no fue confirmada antes de la hora de inicio");
            agendaRepository.eliminar(TipoActividad.ALQUILER, r.getId());
            n++;
        }
        for (SolicitudCatering c : cateringRepository.findByEstadoAndFinBefore(EstadoSolicitudCatering.CONFIRMADA, ahora)) {
            n += cerrarCatering(c, ahora) ? 1 : 0;
        }
        for (SolicitudCatering c : cateringRepository.findByEstadoAndInicioBefore(EstadoSolicitudCatering.PENDIENTE, ahora)) {
            c.setEstado(EstadoSolicitudCatering.CANCELADA);
            c.setMotivoEstado("Cancelada automaticamente: no fue confirmada antes del evento");
            agendaRepository.eliminar(TipoActividad.CATERING, c.getId());
            n++;
        }
        for (Cotizacion c : cotizacionRepository.findByEstadoInAndFechaVencimientoBefore(EstadoCotizacion.ABIERTAS,
                ahora.toLocalDate())) {
            c.setEstado(EstadoCotizacion.RECHAZADA);
            c.setMotivoRechazo("Vencida automaticamente el " + c.getFechaVencimiento());
            c.setFechaDecision(ahora);
            n++;
        }
        if (n > 0) {
            log.info("Cierre automatico: {} proceso(s) actualizados", n);
        }
        return n;
    }

    private boolean cerrarInscripcion(Inscripcion i, LocalDateTime ahora) {
        if (i.getEstado() == EstadoInscripcion.CONFIRMADA && i.getOferta().yaTermino(ahora)
                && pagado(TipoReferenciaPago.INSCRIPCION, i.getId())) {
            i.setEstado(EstadoInscripcion.FINALIZADA);
            i.setFechaCierre(ahora);
            return true;
        }
        return false;
    }

    private boolean cerrarReserva(ReservaEspacio r, LocalDateTime ahora) {
        if (r.getEstado() == EstadoReserva.CONFIRMADA && !ahora.isBefore(r.getFin())
                && pagado(TipoReferenciaPago.ALQUILER, r.getId())) {
            r.setEstado(EstadoReserva.COMPLETADA);
            return true;
        }
        return false;
    }

    private boolean cerrarCatering(SolicitudCatering c, LocalDateTime ahora) {
        if (c.getEstado() == EstadoSolicitudCatering.CONFIRMADA && !ahora.isBefore(c.getFin())
                && pagado(TipoReferenciaPago.CATERING, c.getId())) {
            c.setEstado(EstadoSolicitudCatering.ATENDIDA);
            return true;
        }
        return false;
    }

    private boolean pagado(TipoReferenciaPago tipo, Long id) {
        return pagoRepository.findFirstByTipoReferenciaAndReferenciaIdAndEstadoNot(tipo, id, EstadoPago.ANULADO)
                .map(p -> p.getEstado() == EstadoPago.PAGADO).orElse(false);
    }
}
