package edu.udb.ucacfc.inscripcion;

import edu.udb.ucacfc.academico.oferta.Curso;
import edu.udb.ucacfc.academico.oferta.CursoRepository;
import edu.udb.ucacfc.academico.oferta.Diplomado;
import edu.udb.ucacfc.academico.oferta.DiplomadoRepository;
import edu.udb.ucacfc.academico.oferta.OfertaAcademica;
import edu.udb.ucacfc.academico.oferta.TipoOferta;
import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteService;
import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.inscripcion.dto.InscripcionFiltro;
import edu.udb.ucacfc.inscripcion.dto.InscripcionRequest;
import edu.udb.ucacfc.inscripcion.dto.InscripcionResponse;
import edu.udb.ucacfc.pago.Pago;
import edu.udb.ucacfc.pago.PagoService;
import edu.udb.ucacfc.pago.TipoReferenciaPago;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.CupoExcedidoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static edu.udb.ucacfc.inscripcion.EstadoInscripcion.CANCELADA;
import static edu.udb.ucacfc.inscripcion.EstadoInscripcion.CONFIRMADA;
import static edu.udb.ucacfc.inscripcion.EstadoInscripcion.FINALIZADA;
import static edu.udb.ucacfc.inscripcion.EstadoInscripcion.PENDIENTE;

/**
 * Modulo 3 - Inscripciones. El ROL lo decide @PreAuthorize (confirmar/finalizar: personal; cancelar: personal o el
 * dueno). Aqui viven el DUENO DEL REGISTRO y las reglas de negocio:
 *  - Curso XOR diplomado; cliente y oferta activos; la oferta no ha terminado.
 *  - PERSONA se inscribe a si misma (una vez por oferta); EMPRESA inscribe colaboradores (un DUI una vez por oferta).
 *  - Cupo: PENDIENTE y CONFIRMADA ocupan cupo. La fila de la oferta se bloquea (FOR UPDATE) al contar.
 *  - Confirmar genera la cuenta por cobrar con el monto congelado al inscribirse.
 *  - Cancelar: un CLIENTE solo antes de que inicie la oferta; nunca con dinero cobrado o en validacion.
 *  - Finalizar: la oferta termino y la cuenta esta PAGADA (normalmente lo hace el cierre automatico).
 */
@Service
@RequiredArgsConstructor
public class InscripcionService {

    private static final Map<String, String> SORT = Map.of("id", "id", "estado", "estado",
            "fechaInscripcion", "fechaInscripcion", "monto", "monto", "cliente", "cliente.nombre");
    private static final MaquinaEstados<EstadoInscripcion> MAQUINA = MaquinaEstados.de("inscripcion", EstadoInscripcion.class)
            .permitir(PENDIENTE, CONFIRMADA, CANCELADA)
            .permitir(CONFIRMADA, CANCELADA, FINALIZADA);

    private final InscripcionRepository repo;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;
    private final ClienteService clienteService;
    private final PagoService pagoService;
    private final AccesoService acceso;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<InscripcionResponse> listar(InscripcionFiltro f, PaginaRequest pagina, Usuario usuario) {
        Long clienteId = acceso.filtroCliente(usuario, f == null ? null : f.clienteId());
        Specification<Inscripcion> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (clienteId != null) {
                p.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (f != null) {
                if (f.cursoId() != null) {
                    p.add(cb.equal(root.get("curso").get("id"), f.cursoId()));
                }
                if (f.diplomadoId() != null) {
                    p.add(cb.equal(root.get("diplomado").get("id"), f.diplomadoId()));
                }
                if (f.estado() != null) {
                    p.add(cb.equal(root.get("estado"), f.estado()));
                }
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    String like = "%" + texto.toLowerCase() + "%";
                    p.add(cb.or(cb.like(cb.lower(cb.coalesce(root.<String>get("participanteNombre"), "")), like),
                            cb.like(cb.lower(root.get("cliente").get("nombre")), like)));
                }
                if (f.desde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("fechaInscripcion"), f.desde().atStartOfDay()));
                }
                if (f.hasta() != null) {
                    p.add(cb.lessThan(root.get("fechaInscripcion"), f.hasta().plusDays(1).atStartOfDay()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "id,desc"))
                .map(i -> aResponse(i, usuario)));
    }

    @Transactional(readOnly = true)
    public InscripcionResponse obtener(Long id, Usuario usuario) {
        Inscripcion i = buscar(id);
        acceso.verificarDueno(usuario, i.getCliente().getId(), "una inscripcion");
        return aResponse(i, usuario);
    }

    @Transactional
    public InscripcionResponse crear(InscripcionRequest r, Usuario usuario) {
        Cliente cliente = clienteService.buscarActivo(acceso.resolverClienteParaCrear(usuario, r.clienteId()));
        OfertaAcademica oferta = bloquearOferta(r);
        validarOfertaInscribible(oferta);
        Participante part = validarParticipante(cliente, r);
        validarDuplicado(oferta, cliente, part, -1L);
        validarCupo(oferta);

        Inscripcion i = Inscripcion.builder()
                .cliente(cliente)
                .curso(oferta instanceof Curso c ? c : null)
                .diplomado(oferta instanceof Diplomado d ? d : null)
                .participanteNombre(part.nombre()).participanteDui(part.dui()).participanteCorreo(part.correo())
                .fechaInscripcion(ahora()).estado(PENDIENTE).monto(oferta.getCosto())
                .build();
        return aResponse(repo.save(i), usuario);
    }

    /** Cambiar de oferta o corregir el participante: solo mientras esta PENDIENTE. */
    @Transactional
    public InscripcionResponse actualizar(Long id, InscripcionRequest r, Usuario usuario) {
        Inscripcion i = buscar(id);
        acceso.verificarDueno(usuario, i.getCliente().getId(), "una inscripcion");
        if (i.getEstado() != PENDIENTE) {
            throw new ConflictoEstadoException("Solo se edita una inscripcion PENDIENTE; esta esta " + i.getEstado());
        }
        if (!acceso.esCliente(usuario) && r.clienteId() != null && !r.clienteId().equals(i.getCliente().getId())) {
            throw new ReglaNegocioException("El cliente de una inscripcion no se cambia: cancelala y crea otra");
        }
        OfertaAcademica oferta = bloquearOferta(r);
        validarOfertaInscribible(oferta);
        Participante part = validarParticipante(i.getCliente(), r);
        validarDuplicado(oferta, i.getCliente(), part, id);
        boolean cambiaOferta = !(oferta.getTipo() == i.getTipoOferta() && oferta.getId().equals(i.getOferta().getId()));
        if (cambiaOferta) {
            validarCupo(oferta);
        }
        i.setCurso(oferta instanceof Curso c ? c : null);
        i.setDiplomado(oferta instanceof Diplomado d ? d : null);
        i.setParticipanteNombre(part.nombre());
        i.setParticipanteDui(part.dui());
        i.setParticipanteCorreo(part.correo());
        if (cambiaOferta) {
            i.setMonto(oferta.getCosto());
        }
        return aResponse(repo.save(i), usuario);
    }

    @Transactional
    public InscripcionResponse confirmar(Long id, Usuario usuario) {
        Inscripcion i = buscar(id);
        MAQUINA.validar(i.getEstado(), CONFIRMADA);
        OfertaAcademica o = i.getOferta();
        if (!o.isActivo() || o.yaTermino(ahora())) {
            throw new ConflictoEstadoException("La oferta '" + o.getNombre() + "' ya no esta vigente");
        }
        i.setEstado(CONFIRMADA);
        i.setFechaConfirmacion(ahora());
        repo.save(i);
        pagoService.generarCuenta(TipoReferenciaPago.INSCRIPCION, i.getId(), i.getCliente(),
                "Inscripcion #" + i.getId() + " - " + o.getTipo().name().toLowerCase() + " " + o.getNombre()
                        + " (" + i.nombreParticipante() + ")", i.getMonto());
        return aResponse(i, usuario);
    }

    @Transactional
    public InscripcionResponse cancelar(Long id, String motivo, Usuario usuario) {
        Inscripcion i = buscar(id);
        acceso.verificarDueno(usuario, i.getCliente().getId(), "una inscripcion");
        MAQUINA.validar(i.getEstado(), CANCELADA);
        if (acceso.esCliente(usuario) && i.getOferta().yaInicio(ahora())) {
            throw new ConflictoEstadoException("La oferta ya inicio: solicita la cancelacion en recepcion");
        }
        // Primero el dinero: si hay cobros se lanza la excepcion y no cambia nada (rollback).
        pagoService.anularCuenta(TipoReferenciaPago.INSCRIPCION, id, "Inscripcion cancelada");
        i.setEstado(CANCELADA);
        i.setFechaCierre(ahora());
        i.setMotivoCancelacion(motivo == null || motivo.isBlank()
                ? (acceso.esCliente(usuario) ? "Cancelada por el cliente" : "Cancelada por el CFC") : motivo);
        return aResponse(repo.save(i), usuario);
    }

    /** Respaldo manual del cierre automatico: exige oferta terminada y cuenta PAGADA. */
    @Transactional
    public InscripcionResponse finalizar(Long id, Usuario usuario) {
        Inscripcion i = buscar(id);
        MAQUINA.validar(i.getEstado(), FINALIZADA);
        if (!i.getOferta().yaTermino(ahora())) {
            throw new ConflictoEstadoException("No se puede finalizar: la oferta termina el "
                    + AgendaService.FMT.format(i.getOferta().getFin()));
        }
        pagoService.exigirSaldado(TipoReferenciaPago.INSCRIPCION, id);
        i.setEstado(FINALIZADA);
        i.setFechaCierre(ahora());
        return aResponse(repo.save(i), usuario);
    }

    // ------------------------------------------------------------------------------------------------

    private record Participante(String nombre, String dui, String correo) {
    }

    private OfertaAcademica bloquearOferta(InscripcionRequest r) {
        if ((r.cursoId() == null) == (r.diplomadoId() == null)) {
            throw new ReglaNegocioException("Indica exactamente uno: cursoId o diplomadoId");
        }
        if (r.cursoId() != null) {
            return cursoRepository.bloquear(r.cursoId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Curso", r.cursoId()));
        }
        return diplomadoRepository.bloquear(r.diplomadoId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Diplomado", r.diplomadoId()));
    }

    private void validarOfertaInscribible(OfertaAcademica o) {
        String tipo = o.getTipo().name().toLowerCase();
        if (!o.isActivo()) {
            throw new ReglaNegocioException("El " + tipo + " '" + o.getNombre() + "' esta desactivado y no acepta inscripciones");
        }
        if (o.yaTermino(ahora())) {
            throw new ReglaNegocioException("El " + tipo + " '" + o.getNombre() + "' ya finalizo");
        }
    }

    private Participante validarParticipante(Cliente c, InscripcionRequest r) {
        String nombre = Textos.vacioANull(r.participanteNombre());
        String dui = Textos.vacioANull(r.participanteDui());
        String correo = Textos.vacioANull(r.participanteCorreo());
        if (c.getTipo() == TipoCliente.EMPRESA) {
            if (nombre == null || dui == null) {
                throw new ReglaNegocioException("La empresa debe indicar el nombre y DUI del participante a inscribir");
            }
            return new Participante(nombre, dui, correo == null ? null : correo.toLowerCase());
        }
        if (nombre != null || dui != null || correo != null) {
            throw new ReglaNegocioException("Un cliente PERSONA se inscribe a si mismo: no envies datos de participante");
        }
        return new Participante(null, null, null);
    }

    private void validarDuplicado(OfertaAcademica o, Cliente c, Participante p, Long excluirId) {
        Set<EstadoInscripcion> v = EstadoInscripcion.VIGENTES;
        boolean existe;
        if (p.dui() == null) {
            existe = o.getTipo() == TipoOferta.CURSO
                    ? repo.existsByCursoIdAndClienteIdAndParticipanteDuiIsNullAndEstadoInAndIdNot(o.getId(), c.getId(), v, excluirId)
                    : repo.existsByDiplomadoIdAndClienteIdAndParticipanteDuiIsNullAndEstadoInAndIdNot(o.getId(), c.getId(), v, excluirId);
        } else {
            existe = o.getTipo() == TipoOferta.CURSO
                    ? repo.existsByCursoIdAndParticipanteDuiAndEstadoInAndIdNot(o.getId(), p.dui(), v, excluirId)
                    : repo.existsByDiplomadoIdAndParticipanteDuiAndEstadoInAndIdNot(o.getId(), p.dui(), v, excluirId);
        }
        if (existe) {
            throw new RegistroDuplicadoException((p.dui() == null ? c.getNombre() : "El participante con DUI " + p.dui())
                    + " ya tiene una inscripcion vigente en '" + o.getNombre() + "'");
        }
    }

    private void validarCupo(OfertaAcademica o) {
        long ocupados = o.getTipo() == TipoOferta.CURSO
                ? repo.countByCursoIdAndEstadoIn(o.getId(), EstadoInscripcion.VIGENTES)
                : repo.countByDiplomadoIdAndEstadoIn(o.getId(), EstadoInscripcion.VIGENTES);
        if (ocupados >= o.getCupoMaximo()) {
            throw new CupoExcedidoException("'" + o.getNombre() + "' ya alcanzo su cupo maximo (" + o.getCupoMaximo() + ")");
        }
    }

    private Inscripcion buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Inscripcion", id));
    }

    /** Que puede hacer ESTE usuario con la inscripcion ahora mismo (para que el frontend muestre solo botones validos). */
    private Set<EstadoInscripcion> acciones(Inscripcion i, Usuario u) {
        Set<EstadoInscripcion> s = EnumSet.noneOf(EstadoInscripcion.class);
        s.addAll(MAQUINA.siguientes(i.getEstado()));
        if (acceso.esCliente(u)) {
            s.retainAll(EnumSet.of(CANCELADA));
            if (i.getOferta().yaInicio(ahora())) {
                s.clear();
            }
        }
        if (u.getRol() == edu.udb.ucacfc.seguridad.usuario.Rol.CONTABILIDAD) {
            s.clear();
        }
        return s;
    }

    private InscripcionResponse aResponse(Inscripcion i, Usuario u) {
        OfertaAcademica o = i.getOferta();
        Optional<Pago> pago = pagoService.cuentaActiva(TipoReferenciaPago.INSCRIPCION, i.getId());
        return new InscripcionResponse(i.getId(), i.getCliente().getId(), i.getCliente().nombreMostrado(),
                i.getTipoOferta(), o.getId(), o.getNombre(), o.getInicio(), o.getFin(),
                i.nombreParticipante(), i.getParticipanteDui(), i.getParticipanteCorreo(),
                i.getFechaInscripcion(), i.getEstado(), i.getMonto(),
                pago.map(Pago::getId).orElse(null), pago.map(Pago::getEstado).orElse(null),
                pago.map(Pago::saldo).orElse(null),
                i.getFechaConfirmacion(), i.getFechaCierre(), i.getMotivoCancelacion(), acciones(i, u));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
