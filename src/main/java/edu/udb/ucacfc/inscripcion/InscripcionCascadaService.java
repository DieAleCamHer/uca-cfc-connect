package edu.udb.ucacfc.inscripcion;

import edu.udb.ucacfc.academico.oferta.TipoOferta;
import edu.udb.ucacfc.pago.PagoService;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que el modulo academico necesita saber/hacer sobre inscripciones, sin depender de InscripcionService
 * (evita la dependencia circular Oferta <-> Inscripcion).
 */
@Service
@RequiredArgsConstructor
public class InscripcionCascadaService {

    private final InscripcionRepository repo;
    private final PagoService pagoService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public long vigentes(TipoOferta tipo, Long ofertaId) {
        return tipo == TipoOferta.CURSO
                ? repo.countByCursoIdAndEstadoIn(ofertaId, EstadoInscripcion.VIGENTES)
                : repo.countByDiplomadoIdAndEstadoIn(ofertaId, EstadoInscripcion.VIGENTES);
    }

    /**
     * Baja de una oferta que aun no termina. TODO O NADA:
     *  fase 1 verifica que ninguna inscripcion vigente tenga dinero cobrado o en validacion;
     *  fase 2 cancela todas y anula sus cuentas. Corre dentro de la transaccion de la baja (MANDATORY).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int cancelarPorBajaDeOferta(TipoOferta tipo, Long ofertaId, String nombreOferta) {
        List<Inscripcion> vigentes = tipo == TipoOferta.CURSO
                ? repo.findByCursoIdAndEstadoIn(ofertaId, EstadoInscripcion.VIGENTES)
                : repo.findByDiplomadoIdAndEstadoIn(ofertaId, EstadoInscripcion.VIGENTES);
        for (Inscripcion i : vigentes) {
            try {
                pagoService.verificarSinDinero(TipoReferenciaPago.INSCRIPCION, i.getId());
            } catch (ConflictoEstadoException e) {
                throw new ConflictoEstadoException("No se puede desactivar '" + nombreOferta + "': la inscripcion #"
                        + i.getId() + " (" + i.nombreParticipante() + ") ya tiene dinero cobrado o en validacion. "
                        + "No se cancelo nada. Coordinalo con Contabilidad.");
            }
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        for (Inscripcion i : vigentes) {
            pagoService.anularCuenta(TipoReferenciaPago.INSCRIPCION, i.getId(), "Oferta dada de baja");
            i.setEstado(EstadoInscripcion.CANCELADA);
            i.setFechaCierre(ahora);
            i.setMotivoCancelacion("La oferta '" + nombreOferta + "' fue dada de baja por el CFC");
            repo.save(i);
        }
        return vigentes.size();
    }
}
