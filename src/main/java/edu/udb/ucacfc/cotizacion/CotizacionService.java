package edu.udb.ucacfc.cotizacion;

import edu.udb.ucacfc.academico.oferta.CursoRepository;
import edu.udb.ucacfc.academico.oferta.DiplomadoRepository;
import edu.udb.ucacfc.academico.oferta.OfertaAcademica;
import edu.udb.ucacfc.catering.ServicioCatering;
import edu.udb.ucacfc.catering.ServicioCateringRepository;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteService;
import edu.udb.ucacfc.cotizacion.dto.CotizacionFiltro;
import edu.udb.ucacfc.cotizacion.dto.CotizacionRequest;
import edu.udb.ucacfc.cotizacion.dto.CotizacionResponse;
import edu.udb.ucacfc.cotizacion.dto.ItemCotizacionRequest;
import edu.udb.ucacfc.cotizacion.dto.ItemCotizacionResponse;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static edu.udb.ucacfc.cotizacion.EstadoCotizacion.APROBADA;
import static edu.udb.ucacfc.cotizacion.EstadoCotizacion.EN_PROCESO;
import static edu.udb.ucacfc.cotizacion.EstadoCotizacion.PENDIENTE;
import static edu.udb.ucacfc.cotizacion.EstadoCotizacion.RECHAZADA;

/**
 * Modulo 4 - Cotizaciones (RF05 solicitar, RF06 generar).
 *  - Lineas que referencian el catalogo: el precio sugerido sale del catalogo (curso/diplomado: costo por participante;
 *    espacio: precio por hora; catering: precio por persona). Solo el personal puede negociar precios y descuento.
 *  - El tipo define que lineas admite; SERVICIO_COMBINADO exige al menos dos servicios distintos.
 *  - PENDIENTE -> EN_PROCESO (personal la toma) -> APROBADA (solo ADMIN, lineamiento) | RECHAZADA (con motivo).
 *  - Aprobar exige total > 0, vigencia no vencida, lineas OTRO con precio, y que lo cotizado siga activo; genera la cuenta.
 *  - Un CLIENTE crea, edita (si esta PENDIENTE) y retira las suyas; nunca cambia el estado.
 *  - Vencidas se rechazan solas (cierre automatico).
 */
@Service
@RequiredArgsConstructor
public class CotizacionService {

    private static final Map<String, String> SORT = Map.of("id", "id", "estado", "estado", "tipo", "tipo",
            "total", "total", "fechaSolicitud", "fechaSolicitud", "fechaVencimiento", "fechaVencimiento",
            "cliente", "cliente.nombre");

    private final CotizacionRepository repo;
    private final ClienteService clienteService;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final EspacioRepository espacioRepository;
    private final ServicioCateringRepository servicioRepository;
    private final PagoService pagoService;
    private final AccesoService acceso;
    private final ReglasNegocioProperties reglas;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<CotizacionResponse> listar(CotizacionFiltro f, PaginaRequest pagina, Usuario usuario) {
        Long clienteId = acceso.filtroCliente(usuario, f == null ? null : f.clienteId());
        Specification<Cotizacion> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (clienteId != null) {
                p.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (f != null) {
                if (f.tipo() != null) {
                    p.add(cb.equal(root.get("tipo"), f.tipo()));
                }
                if (f.estado() != null) {
                    p.add(cb.equal(root.get("estado"), f.estado()));
                }
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    p.add(cb.like(cb.lower(root.get("descripcion")), "%" + texto.toLowerCase() + "%"));
                }
                if (f.desde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("fechaSolicitud"), f.desde().atStartOfDay()));
                }
                if (f.hasta() != null) {
                    p.add(cb.lessThan(root.get("fechaSolicitud"), f.hasta().plusDays(1).atStartOfDay()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "id,desc"))
                .map(c -> aResponse(c, usuario)));
    }

    @Transactional(readOnly = true)
    public CotizacionResponse obtener(Long id, Usuario usuario) {
        Cotizacion c = buscar(id);
        acceso.verificarDueno(usuario, c.getCliente().getId(), "una cotizacion");
        return aResponse(c, usuario);
    }

    @Transactional
    public CotizacionResponse crear(CotizacionRequest r, Usuario usuario) {
        Cliente cliente = clienteService.buscarActivo(acceso.resolverClienteParaCrear(usuario, r.clienteId()));
        LocalDateTime ahora = LocalDateTime.now(clock);
        Cotizacion c = Cotizacion.builder().cliente(cliente).estado(PENDIENTE).fechaSolicitud(ahora)
                .fechaVencimiento(ahora.toLocalDate().plusDays(reglas.cotizacionVigenciaDias()))
                .descuento(BigDecimal.ZERO).subtotal(BigDecimal.ZERO).total(BigDecimal.ZERO).build();
        aplicar(c, r, usuario);
        return aResponse(repo.save(c), usuario);
    }

    @Transactional
    public CotizacionResponse actualizar(Long id, CotizacionRequest r, Usuario usuario) {
        Cotizacion c = buscar(id);
        acceso.verificarDueno(usuario, c.getCliente().getId(), "una cotizacion");
        if (acceso.esCliente(usuario) && c.getEstado() != PENDIENTE) {
            throw new ConflictoEstadoException("Ya no puedes editarla: el CFC la esta trabajando (" + c.getEstado() + ")");
        }
        if (!EstadoCotizacion.ABIERTAS.contains(c.getEstado())) {
            throw new ConflictoEstadoException("Una cotizacion " + c.getEstado() + " ya no se puede editar");
        }
        if (!acceso.esCliente(usuario) && r.clienteId() != null && !r.clienteId().equals(c.getCliente().getId())) {
            throw new ReglaNegocioException("El cliente de una cotizacion no se cambia: crea una nueva");
        }
        aplicar(c, r, usuario);
        return aResponse(repo.save(c), usuario);
    }

    /** El personal toma la cotizacion para elaborarla. */
    @Transactional
    public CotizacionResponse enProceso(Long id, Usuario usuario) {
        Cotizacion c = buscar(id);
        if (c.getEstado() != PENDIENTE) {
            throw new ConflictoEstadoException("Solo una cotizacion PENDIENTE pasa a EN_PROCESO; esta esta " + c.getEstado());
        }
        c.setEstado(EN_PROCESO);
        c.setElaboradaPor(usuario.getNombre());
        return aResponse(repo.save(c), usuario);
    }

    /** Solo ADMIN (lineamiento: el Administrador aprueba cotizaciones). Genera la cuenta por cobrar. */
    @Transactional
    public CotizacionResponse aprobar(Long id, Usuario usuario) {
        Cotizacion c = buscar(id);
        if (!EstadoCotizacion.ABIERTAS.contains(c.getEstado())) {
            throw new ConflictoEstadoException("Solo se aprueba una cotizacion PENDIENTE o EN_PROCESO; esta esta " + c.getEstado());
        }
        LocalDate hoy = LocalDate.now(clock);
        if (c.getFechaVencimiento().isBefore(hoy)) {
            throw new ConflictoEstadoException("La cotizacion vencio el " + c.getFechaVencimiento() + ": actualiza su vigencia");
        }
        if (c.getTotal().signum() <= 0) {
            throw new ConflictoEstadoException("No se aprueba una cotizacion con total $0");
        }
        for (ItemCotizacion i : c.getItems()) {
            if (i.getTipoItem() == TipoItemCotizacion.OTRO && i.getPrecioUnitario().signum() <= 0) {
                throw new ConflictoEstadoException("La linea '" + i.getDescripcion() + "' no tiene precio asignado");
            }
            if (i.getTipoItem() != TipoItemCotizacion.OTRO) {
                resolverCatalogo(i.getTipoItem(), i.getReferenciaId());
            }
        }
        c.setEstado(APROBADA);
        c.setAprobadaPor(usuario.getNombre());
        c.setFechaDecision(LocalDateTime.now(clock));
        if (c.getElaboradaPor() == null) {
            c.setElaboradaPor(usuario.getNombre());
        }
        repo.save(c);
        pagoService.generarCuenta(TipoReferenciaPago.COTIZACION, id, c.getCliente(),
                "Cotizacion #" + id + " - " + c.getTipo() + ": " + c.getDescripcion(), c.getTotal());
        return aResponse(c, usuario);
    }

    @Transactional
    public CotizacionResponse rechazar(Long id, String motivo, Usuario usuario) {
        Cotizacion c = buscar(id);
        if (!EstadoCotizacion.ABIERTAS.contains(c.getEstado())) {
            throw new ConflictoEstadoException("Solo se rechaza una cotizacion PENDIENTE o EN_PROCESO; esta esta "
                    + c.getEstado() + (c.getEstado() == APROBADA ? " (usa anular-aprobacion)" : ""));
        }
        return cerrar(c, motivo, usuario);
    }

    /** ADMIN revierte una aprobacion (por ejemplo, el cliente desistio). Solo si su cuenta no tiene dinero. */
    @Transactional
    public CotizacionResponse anularAprobacion(Long id, String motivo, Usuario usuario) {
        Cotizacion c = buscar(id);
        if (c.getEstado() != APROBADA) {
            throw new ConflictoEstadoException("Solo se anula la aprobacion de una cotizacion APROBADA");
        }
        pagoService.anularCuenta(TipoReferenciaPago.COTIZACION, id, "Aprobacion anulada: " + motivo);
        return cerrar(c, motivo, usuario);
    }

    /** El cliente (o el personal en su nombre) retira una solicitud abierta. */
    @Transactional
    public CotizacionResponse retirar(Long id, String motivo, Usuario usuario) {
        Cotizacion c = buscar(id);
        acceso.verificarDueno(usuario, c.getCliente().getId(), "una cotizacion");
        if (!EstadoCotizacion.ABIERTAS.contains(c.getEstado())) {
            throw new ConflictoEstadoException("Solo se retira una cotizacion PENDIENTE o EN_PROCESO; esta esta " + c.getEstado());
        }
        return cerrar(c, motivo == null || motivo.isBlank()
                ? (acceso.esCliente(usuario) ? "Retirada por el cliente" : "Retirada a solicitud del cliente") : motivo, usuario);
    }

    // ------------------------------------------------------------------------------------------------

    private CotizacionResponse cerrar(Cotizacion c, String motivo, Usuario usuario) {
        c.setEstado(RECHAZADA);
        c.setMotivoRechazo(motivo);
        c.setFechaDecision(LocalDateTime.now(clock));
        return aResponse(repo.save(c), usuario);
    }

    private void aplicar(Cotizacion c, CotizacionRequest r, Usuario usuario) {
        boolean personal = !acceso.esCliente(usuario);
        LocalDate hoy = LocalDate.now(clock);
        if (r.fechaEvento() != null && r.fechaEvento().isBefore(hoy)) {
            throw new ReglaNegocioException("La fecha del evento no puede estar en el pasado");
        }
        List<ItemCotizacion> items = new ArrayList<>();
        Set<String> referencias = new HashSet<>();
        Set<TipoItemCotizacion> principales = EnumSet.noneOf(TipoItemCotizacion.class);
        for (ItemCotizacionRequest ir : r.items()) {
            if (!r.tipo().admite(ir.tipoItem())) {
                throw new ReglaNegocioException("Una cotizacion " + r.tipo() + " no admite lineas de tipo " + ir.tipoItem()
                        + ". Admite: " + r.tipo().getPrincipales() + " y OTRO");
            }
            items.add(construirItem(ir, personal));
            if (ir.tipoItem() != TipoItemCotizacion.OTRO) {
                principales.add(ir.tipoItem());
                if (!referencias.add(ir.tipoItem() + "#" + ir.referenciaId())) {
                    throw new ReglaNegocioException("La linea " + ir.tipoItem() + " #" + ir.referenciaId()
                            + " esta repetida: ajusta la cantidad en una sola linea");
                }
            }
        }
        if (principales.isEmpty()) {
            throw new ReglaNegocioException("Agrega al menos una linea de " + r.tipo().getPrincipales());
        }
        if (r.tipo() == TipoCotizacion.SERVICIO_COMBINADO && principales.size() < 2) {
            throw new ReglaNegocioException("Un SERVICIO_COMBINADO debe incluir al menos dos servicios distintos "
                    + "(curso, diplomado, espacio o catering); si es uno solo, usa el tipo especifico");
        }
        BigDecimal subtotal = items.stream().map(ItemCotizacion::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuento = personal && r.descuento() != null ? r.descuento() : (personal ? BigDecimal.ZERO : c.getDescuento());
        if (descuento.compareTo(subtotal) > 0) {
            throw new ReglaNegocioException("El descuento ($" + descuento + ") no puede superar el subtotal ($" + subtotal + ")");
        }
        if (personal && r.fechaVencimiento() != null) {
            if (r.fechaVencimiento().isBefore(hoy)) {
                throw new ReglaNegocioException("La fecha de vencimiento no puede estar en el pasado");
            }
            c.setFechaVencimiento(r.fechaVencimiento());
        }
        c.setTipo(r.tipo());
        c.setDescripcion(r.descripcion());
        c.setFechaEvento(r.fechaEvento());
        c.reemplazarItems(items);
        c.setSubtotal(subtotal);
        c.setDescuento(descuento);
        c.setTotal(subtotal.subtract(descuento));
    }

    private ItemCotizacion construirItem(ItemCotizacionRequest ir, boolean personal) {
        String descripcion = Textos.vacioANull(ir.descripcion());
        BigDecimal precioCatalogo;
        if (ir.tipoItem() == TipoItemCotizacion.OTRO) {
            if (ir.referenciaId() != null) {
                throw new ReglaNegocioException("Una linea OTRO no referencia el catalogo (quita referenciaId)");
            }
            if (descripcion == null) {
                throw new ReglaNegocioException("Describe la linea OTRO (ej. transporte, material impreso)");
            }
            precioCatalogo = BigDecimal.ZERO;
        } else {
            if (ir.referenciaId() == null) {
                throw new ReglaNegocioException("La linea " + ir.tipoItem() + " necesita referenciaId");
            }
            Catalogo cat = resolverCatalogo(ir.tipoItem(), ir.referenciaId());
            precioCatalogo = cat.precio();
            if (descripcion == null) {
                descripcion = cat.descripcion();
            }
        }
        BigDecimal precio = personal && ir.precioUnitario() != null ? ir.precioUnitario() : precioCatalogo;
        return ItemCotizacion.builder().tipoItem(ir.tipoItem()).referenciaId(ir.referenciaId()).descripcion(descripcion)
                .cantidad(ir.cantidad()).precioUnitario(precio)
                .subtotal(precio.multiply(BigDecimal.valueOf(ir.cantidad()))).build();
    }

    private record Catalogo(String descripcion, BigDecimal precio) {
    }

    /** Lo cotizado debe existir y estar activo (y una oferta, no haber terminado). */
    private Catalogo resolverCatalogo(TipoItemCotizacion tipo, Long id) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return switch (tipo) {
            case CURSO -> oferta(cursoRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Curso", id)), ahora);
            case DIPLOMADO -> oferta(diplomadoRepository.findById(id)
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Diplomado", id)), ahora);
            case ESPACIO -> {
                Espacio e = espacioRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", id));
                if (!e.isActivo()) {
                    throw new ReglaNegocioException("El espacio '" + e.getNombre() + "' esta desactivado");
                }
                yield new Catalogo("Alquiler de " + e.getNombre() + " (por hora)", e.getPrecioHora());
            }
            case CATERING -> {
                ServicioCatering s = servicioRepository.findById(id)
                        .orElseThrow(() -> RecursoNoEncontradoException.de("Servicio de catering", id));
                if (!s.isActivo()) {
                    throw new ReglaNegocioException("El servicio '" + s.getNombre() + "' esta desactivado");
                }
                yield new Catalogo(s.getTipo() + ": " + s.getNombre() + " (por persona)", s.getPrecioUnitario());
            }
            case OTRO -> throw new IllegalArgumentException("OTRO no usa catalogo");
        };
    }

    private Catalogo oferta(OfertaAcademica o, LocalDateTime ahora) {
        if (!o.isActivo() || o.yaTermino(ahora)) {
            throw new ReglaNegocioException("El " + o.getTipo().name().toLowerCase() + " '" + o.getNombre()
                    + "' no esta vigente");
        }
        return new Catalogo(o.getTipo().name().charAt(0) + o.getTipo().name().substring(1).toLowerCase() + ": "
                + o.getNombre() + " (por participante)", o.getCosto());
    }

    private Cotizacion buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Cotizacion", id));
    }

    private Set<String> acciones(Cotizacion c, Usuario u) {
        Set<String> a = new LinkedHashSet<>();
        boolean abierta = EstadoCotizacion.ABIERTAS.contains(c.getEstado());
        switch (u.getRol()) {
            case CLIENTE -> {
                if (c.getEstado() == PENDIENTE) {
                    a.add("EDITAR");
                }
                if (abierta) {
                    a.add("RETIRAR");
                }
            }
            case RECEPCIONISTA, ADMIN -> {
                if (abierta) {
                    a.add("EDITAR");
                    a.add("RECHAZAR");
                    a.add("RETIRAR");
                }
                if (c.getEstado() == PENDIENTE) {
                    a.add("EN_PROCESO");
                }
                if (u.getRol() == Rol.ADMIN && abierta) {
                    a.add("APROBAR");
                }
                if (u.getRol() == Rol.ADMIN && c.getEstado() == APROBADA) {
                    a.add("ANULAR_APROBACION");
                }
            }
            default -> {
            }
        }
        return a;
    }

    private CotizacionResponse aResponse(Cotizacion c, Usuario u) {
        Optional<Pago> pago = pagoService.cuentaActiva(TipoReferenciaPago.COTIZACION, c.getId());
        return new CotizacionResponse(c.getId(), c.getCliente().getId(), c.getCliente().nombreMostrado(), c.getTipo(),
                c.getDescripcion(), c.getFechaEvento(), c.getFechaSolicitud(), c.getFechaVencimiento(),
                c.getItems().stream().map(ItemCotizacionResponse::de).toList(), c.getSubtotal(), c.getDescuento(),
                c.getTotal(), c.getEstado(), c.getMotivoRechazo(), c.getElaboradaPor(), c.getAprobadaPor(),
                c.getFechaDecision(), pago.map(Pago::getId).orElse(null), pago.map(Pago::getEstado).orElse(null),
                pago.map(Pago::saldo).orElse(null), acciones(c, u));
    }
}
