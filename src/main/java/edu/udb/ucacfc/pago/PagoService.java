package edu.udb.ucacfc.pago;

import edu.udb.ucacfc.cierre.CierreAutomaticoService;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.pago.dto.AbonoFiltro;
import edu.udb.ucacfc.pago.dto.AbonoRequest;
import edu.udb.ucacfc.pago.dto.AbonoResponse;
import edu.udb.ucacfc.pago.dto.ComprobanteResponse;
import edu.udb.ucacfc.pago.dto.PagoFiltro;
import edu.udb.ucacfc.pago.dto.PagoResponse;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.security.AccesoService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MODULO PAGOS (amplio espectro). Dos caras:
 *  A) API interna para los procesos: generarCuenta, exigirSaldado, verificarSinDinero, anularCuenta.
 *  B) API para personas: listar cuentas, registrar abonos, validar / rechazar / anular abonos, comprobantes.
 *
 * Reglas duras del dinero:
 *  - Una cuenta activa por proceso. El monto lo calcula el proceso (nadie lo escribe a mano).
 *  - Nunca se cobra mas que el saldo (contando lo que esta en validacion).
 *  - No hay reembolsos: un proceso con dinero cobrado o en validacion no se puede cancelar.
 *  - Nada se finaliza sin estar PAGADO.
 *  - Quien registra decide el estado inicial del abono:
 *      CLIENTE      -> solo TRANSFERENCIA/DEPOSITO, quedan PENDIENTE_VALIDACION.
 *      RECEPCIONISTA-> EFECTIVO/TARJETA quedan VALIDADOS (confirmados en caja); TRANSFERENCIA/DEPOSITO a validacion.
 *      CONTABILIDAD / ADMIN -> VALIDADO directamente (son quienes validan).
 */
@Service
@RequiredArgsConstructor
public class PagoService {

    private static final Map<String, String> SORT_PAGO = Map.of("id", "id", "estado", "estado",
            "montoTotal", "montoTotal", "montoPagado", "montoPagado", "tipoReferencia", "tipoReferencia",
            "fechaGeneracion", "fechaCreacion", "cliente", "cliente.nombre");
    private static final Map<String, String> SORT_ABONO = Map.of("id", "id", "monto", "monto", "estado", "estado",
            "metodo", "metodo", "fechaRegistro", "fechaRegistro", "fechaValidacion", "fechaValidacion");

    private final PagoRepository pagoRepository;
    private final AbonoRepository abonoRepository;
    private final CierreAutomaticoService cierre;
    private final AccesoService acceso;
    private final Clock clock;

    // =====================================================================================================
    // A) API interna para los procesos
    // =====================================================================================================

    /** Crea la cuenta por cobrar de un proceso que acaba de volverse cobrable. Monto 0 -> nace PAGADA. */
    @Transactional
    public Pago generarCuenta(TipoReferenciaPago tipo, Long referenciaId, Cliente cliente, String concepto,
                              BigDecimal monto) {
        if (monto == null || monto.signum() < 0) {
            throw new ReglaNegocioException("El monto de la cuenta no puede ser negativo");
        }
        cuentaActiva(tipo, referenciaId).ifPresent(p -> {
            throw new RegistroDuplicadoException(tipo + " #" + referenciaId + " ya tiene la cuenta #" + p.getId());
        });
        Pago p = Pago.builder()
                .tipoReferencia(tipo).referenciaId(referenciaId).cliente(cliente).concepto(concepto)
                .montoTotal(monto).montoPagado(BigDecimal.ZERO).estado(EstadoPago.PENDIENTE)
                .build();
        p.recalcularEstado(ahora());
        return pagoRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Optional<Pago> cuentaActiva(TipoReferenciaPago tipo, Long referenciaId) {
        return pagoRepository.findFirstByTipoReferenciaAndReferenciaIdAndEstadoNot(tipo, referenciaId, EstadoPago.ANULADO);
    }

    @Transactional(readOnly = true)
    public boolean estaSaldado(TipoReferenciaPago tipo, Long referenciaId) {
        return cuentaActiva(tipo, referenciaId).map(p -> p.getEstado() == EstadoPago.PAGADO).orElse(false);
    }

    /** Para cerrar manualmente un proceso: exige la cuenta PAGADA. */
    @Transactional(readOnly = true)
    public void exigirSaldado(TipoReferenciaPago tipo, Long referenciaId) {
        Pago p = cuentaActiva(tipo, referenciaId).orElseThrow(() -> new ConflictoEstadoException(
                tipo + " #" + referenciaId + " no tiene cuenta por cobrar: primero debe confirmarse"));
        if (p.getEstado() != EstadoPago.PAGADO) {
            throw new ConflictoEstadoException("No se puede finalizar " + tipo + " #" + referenciaId + ": su cuenta #"
                    + p.getId() + " esta " + p.getEstado() + " (pagado $" + p.getMontoPagado() + " de $"
                    + p.getMontoTotal() + ")");
        }
    }

    /** No hay reembolsos: si hay dinero validado o reportado, el proceso no se puede cancelar. */
    @Transactional(readOnly = true)
    public void verificarSinDinero(TipoReferenciaPago tipo, Long referenciaId) {
        cuentaActiva(tipo, referenciaId).ifPresent(p -> {
            if (p.getMontoPagado().signum() > 0) {
                throw new ConflictoEstadoException("No se puede cancelar " + tipo + " #" + referenciaId + ": ya tiene $"
                        + p.getMontoPagado() + " cobrados (cuenta #" + p.getId() + "). El sistema no maneja "
                        + "reembolsos: coordinalo con Contabilidad.");
            }
            if (abonoRepository.existsByPagoIdAndEstado(p.getId(), EstadoAbono.PENDIENTE_VALIDACION)) {
                throw new ConflictoEstadoException("No se puede cancelar " + tipo + " #" + referenciaId
                        + ": tiene pagos reportados pendientes de validacion. Contabilidad debe validarlos o rechazarlos primero.");
            }
        });
    }

    /** Cancela la cuenta de un proceso cancelado (si existe). Exige que no haya dinero. */
    @Transactional
    public void anularCuenta(TipoReferenciaPago tipo, Long referenciaId, String motivo) {
        verificarSinDinero(tipo, referenciaId);
        cuentaActiva(tipo, referenciaId).ifPresent(p -> {
            p.setEstado(EstadoPago.ANULADO);
            p.setMotivoAnulacion(motivo == null ? "Proceso cancelado" : motivo);
            pagoRepository.save(p);
        });
    }

    // =====================================================================================================
    // B) API para personas
    // =====================================================================================================

    @Transactional(readOnly = true)
    public PageResponse<PagoResponse> listar(PagoFiltro f, PaginaRequest pagina, Usuario usuario) {
        Long clienteId = acceso.filtroCliente(usuario, f == null ? null : f.clienteId());
        Specification<Pago> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (clienteId != null) {
                p.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (f != null) {
                if (f.estado() != null) {
                    p.add(cb.equal(root.get("estado"), f.estado()));
                }
                if (f.tipoReferencia() != null) {
                    p.add(cb.equal(root.get("tipoReferencia"), f.tipoReferencia()));
                }
                if (f.desde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), f.desde().atStartOfDay()));
                }
                if (f.hasta() != null) {
                    p.add(cb.lessThan(root.get("fechaCreacion"), f.hasta().plusDays(1).atStartOfDay()));
                }
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    p.add(cb.like(cb.lower(root.get("concepto")), "%" + texto.toLowerCase() + "%"));
                }
                if (Boolean.TRUE.equals(f.conSaldo())) {
                    p.add(root.get("estado").in(EstadoPago.PENDIENTE, EstadoPago.PARCIAL));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(pagoRepository.findAll(spec, PaginacionUtil.crear(pagina, SORT_PAGO, "id,desc"))
                .map(p -> PagoResponse.de(p, enValidacion(p), null)));
    }

    @Transactional(readOnly = true)
    public PagoResponse obtener(Long id, Usuario usuario) {
        Pago p = buscar(id);
        acceso.verificarDueno(usuario, p.getCliente().getId(), "una cuenta");
        return detalle(p);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> porReferencia(TipoReferenciaPago tipo, Long referenciaId, Usuario usuario) {
        List<Pago> pagos = pagoRepository.findByTipoReferenciaAndReferenciaIdOrderByIdDesc(tipo, referenciaId);
        pagos.forEach(p -> acceso.verificarDueno(usuario, p.getCliente().getId(), "una cuenta"));
        return pagos.stream().map(this::detalle).toList();
    }

    @Transactional
    public AbonoResponse registrarAbono(Long pagoId, AbonoRequest r, Usuario usuario) {
        Pago p = pagoRepository.bloquear(pagoId).orElseThrow(() -> RecursoNoEncontradoException.de("Cuenta", pagoId));
        acceso.verificarDueno(usuario, p.getCliente().getId(), "una cuenta");
        if (p.getEstado() == EstadoPago.ANULADO) {
            throw new ConflictoEstadoException("La cuenta #" + p.getId() + " esta anulada: su proceso fue cancelado");
        }
        if (p.getEstado() == EstadoPago.PAGADO) {
            throw new ConflictoEstadoException("La cuenta #" + p.getId() + " ya esta pagada por completo");
        }
        boolean esCliente = usuario.getRol() == Rol.CLIENTE;
        if (esCliente && !r.metodo().requiereValidacion()) {
            throw new ReglaNegocioException("Desde tu cuenta solo puedes reportar TRANSFERENCIA o DEPOSITO; "
                    + "el efectivo y la tarjeta se pagan en caja");
        }
        String referencia = Textos.vacioANull(r.referenciaExterna());
        if (r.metodo().requiereValidacion() && referencia == null) {
            throw new ReglaNegocioException("Indica el numero de referencia de la " + r.metodo().name().toLowerCase());
        }
        if (referencia != null && abonoRepository.existsByMetodoAndReferenciaExternaIgnoreCaseAndEstadoIn(
                r.metodo(), referencia, EnumSet.of(EstadoAbono.PENDIENTE_VALIDACION, EstadoAbono.VALIDADO))) {
            throw new RegistroDuplicadoException("La referencia " + referencia + " ya fue registrada en otro pago");
        }
        BigDecimal disponible = p.saldo().subtract(enValidacion(p));
        if (r.monto().compareTo(disponible) > 0) {
            throw new ReglaNegocioException("El abono ($" + r.monto() + ") excede el saldo disponible ($" + disponible
                    + "). Saldo $" + p.saldo() + ", en validacion $" + enValidacion(p));
        }

        boolean validaAlRegistrar = switch (usuario.getRol()) {
            case ADMIN, CONTABILIDAD -> true;
            case RECEPCIONISTA -> !r.metodo().requiereValidacion();
            case CLIENTE -> false;
        };
        Abono a = Abono.builder()
                .pago(p).monto(r.monto()).metodo(r.metodo()).referenciaExterna(referencia)
                .estado(EstadoAbono.PENDIENTE_VALIDACION)
                .registradoPor(usuario.getNombre()).rolRegistro(usuario.getRol())
                .fechaRegistro(ahora())
                .build();
        a = abonoRepository.save(a);
        if (validaAlRegistrar) {
            aplicar(a, p, usuario);
        }
        return AbonoResponse.de(a);
    }

    /** Contabilidad confirma que el dinero de una transferencia/deposito si llego. */
    @Transactional
    public AbonoResponse validarAbono(Long abonoId, Usuario usuario) {
        Abono a = buscarAbono(abonoId);
        Pago p = pagoRepository.bloquear(a.getPago().getId()).orElseThrow();
        if (a.getEstado() != EstadoAbono.PENDIENTE_VALIDACION) {
            throw new ConflictoEstadoException("Solo se validan abonos PENDIENTE_VALIDACION; este esta " + a.getEstado());
        }
        if (p.getEstado() == EstadoPago.ANULADO) {
            throw new ConflictoEstadoException("La cuenta esta anulada: rechaza este abono");
        }
        if (p.getMontoPagado().add(a.getMonto()).compareTo(p.getMontoTotal()) > 0) {
            throw new ConflictoEstadoException("Validarlo excederia el total de la cuenta; rechazalo e indica el motivo");
        }
        aplicar(a, p, usuario);
        return AbonoResponse.de(a);
    }

    @Transactional
    public AbonoResponse rechazarAbono(Long abonoId, String motivo, Usuario usuario) {
        Abono a = buscarAbono(abonoId);
        if (a.getEstado() != EstadoAbono.PENDIENTE_VALIDACION) {
            throw new ConflictoEstadoException("Solo se rechazan abonos PENDIENTE_VALIDACION; este esta " + a.getEstado());
        }
        a.setEstado(EstadoAbono.RECHAZADO);
        a.setMotivo(motivo);
        a.setValidadoPor(usuario.getNombre());
        a.setFechaValidacion(ahora());
        return AbonoResponse.de(abonoRepository.save(a));
    }

    /**
     * Revierte un abono VALIDADO registrado por error (no es un reembolso: el dinero nunca debio contarse).
     * No se permite si el proceso ya se cerro (finalizada/completada/atendida).
     */
    @Transactional
    public AbonoResponse anularAbono(Long abonoId, String motivo, Usuario usuario) {
        Abono a = buscarAbono(abonoId);
        Pago p = pagoRepository.bloquear(a.getPago().getId()).orElseThrow();
        if (a.getEstado() != EstadoAbono.VALIDADO) {
            throw new ConflictoEstadoException("Solo se anulan abonos VALIDADOS; este esta " + a.getEstado());
        }
        if (cierre.procesoCerrado(p.getTipoReferencia(), p.getReferenciaId())) {
            throw new ConflictoEstadoException("El proceso " + p.getTipoReferencia() + " #" + p.getReferenciaId()
                    + " ya fue cerrado; su pago no se puede revertir");
        }
        a.setEstado(EstadoAbono.ANULADO);
        a.setMotivo(motivo);
        p.setMontoPagado(p.getMontoPagado().subtract(a.getMonto()));
        p.recalcularEstado(ahora());
        pagoRepository.save(p);
        return AbonoResponse.de(abonoRepository.save(a));
    }

    @Transactional(readOnly = true)
    public PageResponse<AbonoResponse> listarAbonos(AbonoFiltro f, PaginaRequest pagina, Usuario usuario) {
        Long clienteId = acceso.esCliente(usuario) ? acceso.clienteIdDe(usuario) : null;
        Specification<Abono> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (clienteId != null) {
                p.add(cb.equal(root.get("pago").get("cliente").get("id"), clienteId));
            }
            if (f != null) {
                if (f.estado() != null) {
                    p.add(cb.equal(root.get("estado"), f.estado()));
                }
                if (f.metodo() != null) {
                    p.add(cb.equal(root.get("metodo"), f.metodo()));
                }
                if (f.pagoId() != null) {
                    p.add(cb.equal(root.get("pago").get("id"), f.pagoId()));
                }
                if (f.desde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), f.desde().atStartOfDay()));
                }
                if (f.hasta() != null) {
                    p.add(cb.lessThan(root.get("fechaRegistro"), f.hasta().plusDays(1).atStartOfDay()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(abonoRepository.findAll(spec, PaginacionUtil.crear(pagina, SORT_ABONO, "id,desc"))
                .map(AbonoResponse::de));
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse comprobante(Long abonoId, Usuario usuario) {
        Abono a = buscarAbono(abonoId);
        Pago p = a.getPago();
        acceso.verificarDueno(usuario, p.getCliente().getId(), "un comprobante");
        if (a.getEstado() != EstadoAbono.VALIDADO) {
            throw new ConflictoEstadoException("Solo los abonos VALIDADOS tienen comprobante; este esta " + a.getEstado());
        }
        Cliente c = p.getCliente();
        return new ComprobanteResponse(a.getNumeroComprobante(), a.getFechaValidacion(),
                "Centro de Formacion Continua - UCA", c.nombreMostrado(), c.getDuiNit(), c.getCorreo(),
                p.getTipoReferencia(), p.getReferenciaId(), p.getConcepto(), a.getMonto(), a.getMetodo(),
                a.getReferenciaExterna(), p.getMontoTotal(), p.getMontoPagado(), p.saldo(), p.getEstado(),
                a.getRegistradoPor(), a.getValidadoPor());
    }

    // =====================================================================================================

    /** Suma el abono a la cuenta, emite comprobante y, si quedo PAGADA, intenta cerrar el proceso. */
    private void aplicar(Abono a, Pago p, Usuario usuario) {
        LocalDateTime ahora = ahora();
        a.setEstado(EstadoAbono.VALIDADO);
        a.setValidadoPor(usuario.getNombre());
        a.setFechaValidacion(ahora);
        a.setNumeroComprobante("CFC-" + ahora.getYear() + "-" + String.format("%06d", a.getId()));
        abonoRepository.save(a);

        p.setMontoPagado(p.getMontoPagado().add(a.getMonto()));
        p.recalcularEstado(ahora);
        pagoRepository.save(p);
        if (p.getEstado() == EstadoPago.PAGADO) {
            cierre.alSaldarse(p.getTipoReferencia(), p.getReferenciaId());
        }
    }

    private BigDecimal enValidacion(Pago p) {
        return abonoRepository.sumarPorEstado(p.getId(), EstadoAbono.PENDIENTE_VALIDACION);
    }

    private PagoResponse detalle(Pago p) {
        List<AbonoResponse> abonos = abonoRepository.findByPagoIdOrderByFechaRegistroAsc(p.getId()).stream()
                .map(AbonoResponse::de).toList();
        return PagoResponse.de(p, enValidacion(p), abonos);
    }

    private Pago buscar(Long id) {
        return pagoRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Cuenta", id));
    }

    private Abono buscarAbono(Long id) {
        return abonoRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Abono", id));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
