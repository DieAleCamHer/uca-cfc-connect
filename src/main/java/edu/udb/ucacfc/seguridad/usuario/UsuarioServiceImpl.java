package edu.udb.ucacfc.seguridad.usuario;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.seguridad.usuario.dto.CambiarRolRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.PasswordAdminRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.RolResponse;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioActualizarRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioCrearRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioFiltro;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioResponse;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Administracion de cuentas (solo ADMIN, lo exige @PreAuthorize en el controller).
 * Reglas que protegen al sistema de quedarse sin administrador o con cuentas incoherentes:
 *  - Desde aqui solo se crean cuentas de PERSONAL; una cuenta CLIENTE nace siempre junto a su ficha de cliente.
 *  - No se puede convertir una cuenta de personal en CLIENTE ni al reves (la ficha de cliente quedaria huerfana).
 *  - Nadie puede cambiarse el rol ni desactivarse a si mismo, y nunca se deja al sistema sin un ADMIN activo.
 *  - Cambiar rol, desactivar o restablecer contrasena revoca los tokens de esa cuenta al instante.
 */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final Map<String, String> SORT = Map.of(
            "id", "id", "nombre", "nombre", "email", "email", "rol", "rol", "activo", "activo",
            "ultimoAcceso", "ultimoAcceso", "fechaCreacion", "fechaCreacion");

    private final UsuarioRepository repo;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(UsuarioFiltro f, PaginaRequest pagina) {
        Specification<Usuario> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            String texto = f == null ? null : Textos.vacioANull(f.q());
            if (texto != null) {
                String like = "%" + texto.toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("nombre")), like), cb.like(cb.lower(root.get("email")), like)));
            }
            if (f != null && f.rol() != null) {
                p.add(cb.equal(root.get("rol"), f.rol()));
            }
            if (f != null && f.activo() != null) {
                p.add(cb.equal(root.get("activo"), f.activo()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "id,desc")).map(this::aResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioCrearRequest r) {
        if (!r.rol().esPersonal()) {
            throw new ReglaNegocioException("Las cuentas CLIENTE se crean con el registro publico o desde "
                    + "Clientes > Crear acceso, porque siempre van unidas a una ficha de cliente");
        }
        if (repo.existsByEmailIgnoreCase(r.email())) {
            throw new RegistroDuplicadoException("Ya existe una cuenta con el correo " + r.email());
        }
        Usuario u = repo.save(Usuario.builder()
                .nombre(r.nombre())
                .email(r.email().toLowerCase())
                .password(passwordEncoder.encode(r.password()))
                .rol(r.rol())
                .build());
        return aResponse(u);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest r) {
        Usuario u = buscar(id);
        if (repo.existsByEmailIgnoreCaseAndIdNot(r.email(), id)) {
            throw new RegistroDuplicadoException("Ya existe otra cuenta con el correo " + r.email());
        }
        boolean cambiaCorreo = !u.getEmail().equalsIgnoreCase(r.email());
        u.setNombre(r.nombre());
        u.setEmail(r.email().toLowerCase());
        if (cambiaCorreo) {
            // El correo es el "subject" del token: las sesiones abiertas dejan de ser validas
            u.revocarTokens();
        }
        return aResponse(repo.save(u));
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarRol(Long id, CambiarRolRequest r, Usuario actual) {
        Usuario u = buscar(id);
        if (u.getId().equals(actual.getId())) {
            throw new ConflictoEstadoException("No puedes cambiar tu propio rol: pide a otro administrador que lo haga");
        }
        if (!u.getRol().esPersonal() || !r.rol().esPersonal()) {
            throw new ReglaNegocioException("Solo se cambia el rol entre cuentas de personal (ADMIN, RECEPCIONISTA, "
                    + "CONTABILIDAD). Una cuenta CLIENTE va unida a su ficha de cliente.");
        }
        if (u.getRol() == r.rol()) {
            throw new ReglaNegocioException("La cuenta ya tiene el rol " + r.rol());
        }
        if (u.getRol() == Rol.ADMIN && u.isActivo() && repo.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            throw new ConflictoEstadoException("No puedes quitar el rol al ultimo ADMIN activo del sistema");
        }
        u.setRol(r.rol());
        u.revocarTokens();
        return aResponse(repo.save(u));
    }

    @Override
    @Transactional
    public void desactivar(Long id, Usuario actual) {
        Usuario u = buscar(id);
        if (u.getId().equals(actual.getId())) {
            throw new ConflictoEstadoException("No puedes desactivar tu propia cuenta");
        }
        if (!u.isActivo()) {
            throw new ConflictoEstadoException("La cuenta ya esta desactivada");
        }
        if (u.getRol() == Rol.ADMIN && repo.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            throw new ConflictoEstadoException("No puedes desactivar al ultimo ADMIN activo del sistema");
        }
        u.setActivo(false);
        u.revocarTokens();
        repo.save(u);
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Usuario u = buscar(id);
        if (u.isActivo()) {
            throw new ConflictoEstadoException("La cuenta ya esta activa");
        }
        if (u.getRol() == Rol.CLIENTE) {
            clienteRepository.findByUsuarioId(u.getId()).filter(c -> !c.isActivo()).ifPresent(c -> {
                throw new ConflictoEstadoException("La ficha de cliente de esta cuenta esta desactivada: "
                        + "reactiva primero el cliente #" + c.getId());
            });
        }
        u.setActivo(true);
        repo.save(u);
    }

    @Override
    @Transactional
    public void restablecerPassword(Long id, PasswordAdminRequest r) {
        Usuario u = buscar(id);
        u.setPassword(passwordEncoder.encode(r.nuevaPassword()));
        u.setResetTokenHash(null);
        u.setResetTokenExpira(null);
        u.revocarTokens();
        repo.save(u);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponse> roles() {
        return Arrays.stream(Rol.values())
                .map(r -> new RolResponse(r, r.getNombre(), r.getDescripcion(), r.getPermisos(), r.esPersonal(),
                        repo.countByRolAndActivoTrue(r)))
                .toList();
    }

    private Usuario buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
    }

    private UsuarioResponse aResponse(Usuario u) {
        Long clienteId = u.getRol() == Rol.CLIENTE
                ? clienteRepository.findByUsuarioId(u.getId()).map(Cliente::getId).orElse(null) : null;
        return UsuarioResponse.de(u, clienteId);
    }
}
