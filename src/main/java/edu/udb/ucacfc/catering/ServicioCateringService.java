package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.catering.dto.ServicioCateringFiltro;
import edu.udb.ucacfc.catering.dto.ServicioCateringRequest;
import edu.udb.ucacfc.catering.dto.ServicioCateringResponse;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
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
import java.util.List;
import java.util.Map;

/** Catalogo de catering. tipo + nombre unicos. No se desactiva con pedidos vigentes a futuro. */
@Service
@RequiredArgsConstructor
public class ServicioCateringService {

    private static final Map<String, String> SORT = Map.of("id", "id", "tipo", "tipo", "nombre", "nombre",
            "precioUnitario", "precioUnitario", "activo", "activo");

    private final ServicioCateringRepository repo;
    private final SolicitudCateringRepository solicitudRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<ServicioCateringResponse> listar(ServicioCateringFiltro f, PaginaRequest pagina) {
        Specification<ServicioCatering> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    String like = "%" + texto.toLowerCase() + "%";
                    p.add(cb.or(cb.like(cb.lower(root.get("nombre")), like),
                            cb.like(cb.lower(cb.coalesce(root.<String>get("menuBase"), "")), like)));
                }
                if (f.tipo() != null) {
                    p.add(cb.equal(root.get("tipo"), f.tipo()));
                }
                if (f.activo() != null) {
                    p.add(cb.equal(root.get("activo"), f.activo()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(repo.findAll(spec, PaginacionUtil.crear(pagina, SORT, "tipo,asc;nombre,asc"))
                .map(ServicioCateringResponse::de));
    }

    @Transactional(readOnly = true)
    public ServicioCateringResponse obtener(Long id) {
        return ServicioCateringResponse.de(buscar(id));
    }

    @Transactional
    public ServicioCateringResponse crear(ServicioCateringRequest r) {
        if (repo.existsByTipoAndNombreIgnoreCase(r.tipo(), r.nombre())) {
            throw new RegistroDuplicadoException("Ya existe el servicio " + r.tipo() + " '" + r.nombre() + "'");
        }
        return ServicioCateringResponse.de(repo.save(ServicioCatering.builder().tipo(r.tipo()).nombre(r.nombre())
                .menuBase(r.menuBase()).precioUnitario(r.precioUnitario()).build()));
    }

    @Transactional
    public ServicioCateringResponse actualizar(Long id, ServicioCateringRequest r) {
        ServicioCatering s = buscar(id);
        if (repo.existsByTipoAndNombreIgnoreCaseAndIdNot(r.tipo(), r.nombre(), id)) {
            throw new RegistroDuplicadoException("Ya existe otro servicio " + r.tipo() + " '" + r.nombre() + "'");
        }
        s.setTipo(r.tipo());
        s.setNombre(r.nombre());
        s.setMenuBase(r.menuBase());
        s.setPrecioUnitario(r.precioUnitario());
        return ServicioCateringResponse.de(repo.save(s));
    }

    @Transactional
    public void desactivar(Long id) {
        ServicioCatering s = buscar(id);
        if (!s.isActivo()) {
            throw new ConflictoEstadoException("El servicio ya esta desactivado");
        }
        long vigentes = solicitudRepository.countByServicioIdAndEstadoInAndInicioAfter(id,
                EstadoSolicitudCatering.VIGENTES, LocalDateTime.now(clock));
        if (vigentes > 0) {
            throw new ConflictoEstadoException("El servicio tiene " + vigentes + " pedido(s) vigente(s) a futuro");
        }
        s.setActivo(false);
        repo.save(s);
    }

    @Transactional
    public void activar(Long id) {
        ServicioCatering s = buscar(id);
        if (s.isActivo()) {
            throw new ConflictoEstadoException("El servicio ya esta activo");
        }
        s.setActivo(true);
        repo.save(s);
    }

    public ServicioCatering buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Servicio de catering", id));
    }
}
