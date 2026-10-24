package edu.udb.ucacfc.cliente;

import edu.udb.ucacfc.catering.EstadoSolicitudCatering;
import edu.udb.ucacfc.catering.SolicitudCateringRepository;
import edu.udb.ucacfc.cliente.dto.ClienteFiltro;
import edu.udb.ucacfc.cliente.dto.ClientePerfilRequest;
import edu.udb.ucacfc.cliente.dto.ClienteRequest;
import edu.udb.ucacfc.cliente.dto.ClienteResponse;
import edu.udb.ucacfc.cliente.dto.CrearCuentaRequest;
import edu.udb.ucacfc.cliente.dto.HistorialClienteResponse;
import edu.udb.ucacfc.cliente.dto.HistorialClienteResponse.Movimiento;
import edu.udb.ucacfc.cotizacion.CotizacionRepository;
import edu.udb.ucacfc.cotizacion.EstadoCotizacion;
import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import edu.udb.ucacfc.espacio.reserva.ReservaEspacioRepository;
import edu.udb.ucacfc.inscripcion.EstadoInscripcion;
import edu.udb.ucacfc.inscripcion.InscripcionRepository;
import edu.udb.ucacfc.pago.EstadoPago;
import edu.udb.ucacfc.pago.Pago;
import edu.udb.ucacfc.pago.PagoRepository;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.seguridad.usuario.UsuarioRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modulo 2 - Gestion de clientes (registro, actualizacion, busqueda) + historial (RF14).
 * Reglas agregadas para coherencia:
 *  - El tipo (PERSONA/EMPRESA) no cambia despues de creado: las inscripciones de empresa llevan participantes.
 *  - No se desactiva un cliente con procesos vigentes ni con saldo pendiente; desactivarlo tambien bloquea su
 *    cuenta de acceso, y no podra iniciar procesos nuevos.
 *  - Una ficha tiene a lo sumo una cuenta de acceso (rol CLIENTE).
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final Map<String, String> SORT = Map.of("id", "id", "nombre", "nombre", "empresa", "empresa",
            "duiNit", "duiNit", "correo", "correo", "tipo", "tipo", "activo", "activo", "fechaCreacion", "fechaCreacion");

    private final ClienteRepository repo;
    private final ClienteValidador validador;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccesoService acceso;
    private final InscripcionRepository inscripcionRepository;
    private final ReservaEspacioRepository reservaRepository;
    private final SolicitudCateringRepository cateringRepository;
    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> listar(ClienteFiltro f, PaginaRequest pagina) {
        Specification<Cliente> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    String like = "%" + texto.toLowerCase() + "%";
                    p.add(cb.or(cb.like(cb.lower(root.get("nombre")), like),
                            cb.like(cb.lower(cb.coalesce(root.<String>get("empresa"), "")), like),
                            cb.like(cb.lower(root.get("duiNit")), like),
                            cb.like(cb.lower(root.get("correo")), like)));
                }
                if (f.tipo() != null) {
                    p.add(cb.equal(root.get("tipo"), f.tipo()));
                }
                if (f.activo() != null) {
                    p.add(cb.equal(root.get("activo"), f.activo()));
                }
                if (f.conCuenta() != null) {
                    p.add(f.conCuenta() ? cb.isNotNull(root.get("usuario")) : cb.isNull(root.get("usuario")));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "nombre,asc")).map(ClienteResponse::de));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id, Usuario usuario) {
        acceso.verificarDueno(usuario, id, "la ficha");
        return ClienteResponse.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public ClienteResponse miFicha(Usuario usuario) {
        return ClienteResponse.de(acceso.clienteDe(usuario));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest r) {
        validador.validarTipo(r.tipo(), r.duiNit(), r.empresa());
        validador.validarUnicidad(r.duiNit(), r.correo(), vacio(r.telefono()), null);
        Cliente c = Cliente.builder()
                .tipo(r.tipo()).duiNit(r.duiNit()).nombre(r.nombre())
                .empresa(r.tipo() == TipoCliente.EMPRESA ? r.empresa() : null)
                .correo(r.correo().toLowerCase()).telefono(vacio(r.telefono())).direccion(r.direccion())
                .build();
        return ClienteResponse.de(repo.save(c));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest r) {
        Cliente c = buscar(id);
        if (c.getTipo() != r.tipo()) {
            throw new ConflictoEstadoException("El tipo de cliente no se puede cambiar (" + c.getTipo()
                    + "). Si es otra persona o empresa, registrala como cliente nuevo.");
        }
        validador.validarTipo(r.tipo(), r.duiNit(), r.empresa());
        validador.validarUnicidad(r.duiNit(), r.correo(), vacio(r.telefono()), id);
        c.setDuiNit(r.duiNit());
        c.setNombre(r.nombre());
        c.setEmpresa(r.tipo() == TipoCliente.EMPRESA ? r.empresa() : null);
        c.setCorreo(r.correo().toLowerCase());
        c.setTelefono(vacio(r.telefono()));
        c.setDireccion(r.direccion());
        return ClienteResponse.de(repo.save(c));
    }

    @Transactional
    public ClienteResponse actualizarMiFicha(Usuario usuario, ClientePerfilRequest r) {
        Cliente c = repo.findById(acceso.clienteIdDe(usuario)).orElseThrow();
        if (c.getTipo() == TipoCliente.EMPRESA && (r.empresa() == null || r.empresa().isBlank())) {
            throw new ReglaNegocioException("Indica la razon social de la empresa");
        }
        validador.validarUnicidad(c.getDuiNit(), r.correo(), r.telefono(), c.getId());
        c.setNombre(r.nombre());
        c.setEmpresa(c.getTipo() == TipoCliente.EMPRESA ? r.empresa() : null);
        c.setCorreo(r.correo().toLowerCase());
        c.setTelefono(r.telefono());
        c.setDireccion(r.direccion());
        return ClienteResponse.de(repo.save(c));
    }

    @Transactional
    public void desactivar(Long id) {
        Cliente c = buscar(id);
        if (!c.isActivo()) {
            throw new ConflictoEstadoException("El cliente ya esta desactivado");
        }
        long vigentes = inscripcionRepository.countByClienteIdAndEstadoIn(id, EstadoInscripcion.VIGENTES)
                + reservaRepository.countByClienteIdAndEstadoIn(id, EstadoReserva.VIGENTES)
                + cateringRepository.countByClienteIdAndEstadoIn(id, EstadoSolicitudCatering.VIGENTES)
                + cotizacionRepository.countByClienteIdAndEstadoIn(id, EstadoCotizacion.ABIERTAS);
        if (vigentes > 0) {
            throw new ConflictoEstadoException("El cliente tiene " + vigentes + " proceso(s) vigente(s) "
                    + "(inscripciones, reservas, catering o cotizaciones abiertas). Cancelalos o cierralos primero.");
        }
        BigDecimal saldo = saldoPendiente(id);
        if (saldo.signum() > 0) {
            throw new ConflictoEstadoException("El cliente tiene un saldo pendiente de $" + saldo + ". No se puede desactivar.");
        }
        c.setActivo(false);
        if (c.getUsuario() != null) {
            Usuario u = c.getUsuario();
            u.setActivo(false);
            u.revocarTokens();
            usuarioRepository.save(u);
        }
        repo.save(c);
    }

    @Transactional
    public void activar(Long id) {
        Cliente c = buscar(id);
        if (c.isActivo()) {
            throw new ConflictoEstadoException("El cliente ya esta activo");
        }
        c.setActivo(true);
        if (c.getUsuario() != null) {
            c.getUsuario().setActivo(true);
            usuarioRepository.save(c.getUsuario());
        }
        repo.save(c);
    }

    /** Recepcion habilita el acceso web de un cliente de mostrador. */
    @Transactional
    public ClienteResponse crearCuenta(Long id, CrearCuentaRequest r) {
        Cliente c = buscar(id);
        if (!c.isActivo()) {
            throw new ConflictoEstadoException("El cliente esta desactivado");
        }
        if (c.getUsuario() != null) {
            throw new RegistroDuplicadoException("Este cliente ya tiene cuenta de acceso (" + c.getUsuario().getEmail() + ")");
        }
        String email = (Textos.vacioANull(r.email()) == null ? c.getCorreo() : r.email()).toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegistroDuplicadoException("Ya existe una cuenta con el correo " + email);
        }
        Usuario u = usuarioRepository.save(Usuario.builder().nombre(c.getNombre()).email(email)
                .password(passwordEncoder.encode(r.password())).rol(Rol.CLIENTE).build());
        c.setUsuario(u);
        return ClienteResponse.de(repo.save(c));
    }

    @Transactional(readOnly = true)
    public HistorialClienteResponse historial(Long id, Usuario usuario) {
        acceso.verificarDueno(usuario, id, "el historial");
        Cliente c = buscar(id);
        Map<String, Long> ins = new LinkedHashMap<>();
        for (EstadoInscripcion e : EstadoInscripcion.values()) {
            ins.put(e.name(), inscripcionRepository.countByClienteIdAndEstado(id, e));
        }
        Map<String, Long> res = new LinkedHashMap<>();
        for (EstadoReserva e : EstadoReserva.values()) {
            res.put(e.name(), reservaRepository.countByClienteIdAndEstado(id, e));
        }
        Map<String, Long> cat = new LinkedHashMap<>();
        for (EstadoSolicitudCatering e : EstadoSolicitudCatering.values()) {
            cat.put(e.name(), cateringRepository.countByClienteIdAndEstado(id, e));
        }
        Map<String, Long> cot = new LinkedHashMap<>();
        for (EstadoCotizacion e : EstadoCotizacion.values()) {
            cot.put(e.name(), cotizacionRepository.countByClienteIdAndEstado(id, e));
        }
        List<Pago> cuentas = pagoRepository.findByClienteIdAndEstadoNot(id, EstadoPago.ANULADO);
        BigDecimal facturado = cuentas.stream().map(Pago::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pagado = cuentas.stream().map(Pago::getMontoPagado).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Movimiento> mov = new ArrayList<>();
        inscripcionRepository.findTop10ByClienteIdOrderByIdDesc(id).forEach(i -> mov.add(new Movimiento("INSCRIPCION",
                i.getId(), i.getTipoOferta().name().toLowerCase() + " " + i.getOferta().getNombre() + " - "
                + i.nombreParticipante(), i.getFechaInscripcion(), i.getEstado().name(), i.getMonto())));
        reservaRepository.findTop10ByClienteIdOrderByIdDesc(id).forEach(r -> mov.add(new Movimiento("ALQUILER",
                r.getId(), r.getEspacio().getNombre() + " - " + r.getProposito(), r.getInicio(), r.getEstado().name(),
                r.getTotal())));
        cateringRepository.findTop10ByClienteIdOrderByIdDesc(id).forEach(s -> mov.add(new Movimiento("CATERING",
                s.getId(), s.getServicio().getNombre() + " x " + s.getNumeroAsistentes(), s.getInicio(),
                s.getEstado().name(), s.getTotal())));
        cotizacionRepository.findTop10ByClienteIdOrderByIdDesc(id).forEach(q -> mov.add(new Movimiento("COTIZACION",
                q.getId(), q.getTipo() + " - " + q.getDescripcion(), q.getFechaSolicitud(), q.getEstado().name(),
                q.getTotal())));
        mov.sort(Comparator.comparing(Movimiento::fecha, Comparator.nullsLast(Comparator.reverseOrder())));

        return new HistorialClienteResponse(ClienteResponse.de(c), ins, res, cat, cot, facturado, pagado,
                facturado.subtract(pagado), mov.size() > 20 ? mov.subList(0, 20) : mov);
    }

    public Cliente buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", id));
    }

    /** Cliente activo para iniciar un proceso nuevo. */
    public Cliente buscarActivo(Long id) {
        Cliente c = buscar(id);
        if (!c.isActivo()) {
            throw new ReglaNegocioException("El cliente '" + c.nombreMostrado() + "' esta desactivado");
        }
        return c;
    }

    private BigDecimal saldoPendiente(Long clienteId) {
        return pagoRepository.findByClienteIdAndEstadoNot(clienteId, EstadoPago.ANULADO).stream()
                .map(Pago::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String vacio(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
