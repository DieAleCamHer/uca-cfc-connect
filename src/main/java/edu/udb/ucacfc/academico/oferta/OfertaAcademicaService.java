package edu.udb.ucacfc.academico.oferta;

import edu.udb.ucacfc.academico.categoria.Categoria;
import edu.udb.ucacfc.academico.docente.Docente;
import edu.udb.ucacfc.academico.modalidad.Modalidad;
import edu.udb.ucacfc.academico.oferta.dto.OfertaFiltro;
import edu.udb.ucacfc.academico.oferta.dto.OfertaRequest;
import edu.udb.ucacfc.academico.oferta.dto.OfertaResponse;
import edu.udb.ucacfc.academico.oferta.dto.SesionRequest;
import edu.udb.ucacfc.academico.oferta.dto.SesionResponse;
import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.agenda.NuevaActividad;
import edu.udb.ucacfc.agenda.TipoActividad;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.inscripcion.EstadoInscripcion;
import edu.udb.ucacfc.inscripcion.Inscripcion;
import edu.udb.ucacfc.shared.exception.ConflictoEstadoException;
import edu.udb.ucacfc.shared.exception.DocenteOcupadoException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.util.Horarios;
import edu.udb.ucacfc.shared.util.Textos;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import edu.udb.ucacfc.shared.web.PaginacionUtil;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Logica COMPARTIDA de Curso y Diplomado (lineamiento modulo 1: CRUD, activar/inactivar, cupo, fechas, horarios, costos).
 *
 * Reglas al CREAR (y al editar una oferta que aun no inicia):
 *  1. 1 a 200 sesiones; cada una dura entre 30 min y 12 h; no se cruzan entre si; todas en el futuro.
 *  2. Categoria, modalidad y docente existen y estan activos.
 *  3. Modalidad presencial -> espacio obligatorio, activo y con capacidad >= cupo. Virtual -> sin espacio.
 *  4. Nombre + categoria + fecha de inicio unicos (otra fecha = otra edicion).
 *  5. El docente no puede tener otra oferta vigente con sesiones que se crucen (EspacioOcupado tiene su equivalente:
 *     DocenteOcupadoException). La fila del docente se bloquea mientras se valida.
 *  6. El espacio se ocupa en la agenda UNA VEZ POR SESION (la agenda valida cruces con todo lo demas).
 * Al EDITAR una oferta que ya inicio solo cambian nombre, descripcion, cupo y costo. Finalizada o inactiva: solo lectura.
 * El cupo nunca baja de las inscripciones vigentes. El costo nuevo solo aplica a inscripciones futuras
 * (cada inscripcion congela su monto).
 * DESACTIVAR (antes de terminar) cancela en cascada inscripciones y cuentas sin dinero y libera la agenda: todo o nada.
 * REACTIVAR revalida catalogos, docente y espacio y vuelve a ocupar la agenda con las sesiones futuras.
 */
public abstract class OfertaAcademicaService<T extends OfertaAcademica> {

    private static final Map<String, String> SORT = Map.of("id", "id", "nombre", "nombre", "inicio", "inicio",
            "fin", "fin", "costo", "costo", "cupoMaximo", "cupoMaximo", "activo", "activo",
            "categoria", "categoria.nombre", "docente", "docente.nombre", "modalidad", "modalidad.nombre");
    private static final Duration MIN_SESION = Duration.ofMinutes(30);
    private static final Duration MAX_SESION = Duration.ofHours(12);

    protected final OfertaRepository<T> repo;
    protected final OfertaDependencias deps;

    protected OfertaAcademicaService(OfertaRepository<T> repo, OfertaDependencias deps) {
        this.repo = repo;
        this.deps = deps;
    }

    protected abstract T nueva();

    protected abstract TipoOferta tipo();

    /** "curso" o "diplomado" para los mensajes. */
    protected abstract String etiqueta();

    private TipoActividad tipoActividad() {
        return tipo() == TipoOferta.CURSO ? TipoActividad.CURSO : TipoActividad.DIPLOMADO;
    }

    private String titulo() {
        String e = etiqueta();
        return Character.toUpperCase(e.charAt(0)) + e.substring(1);
    }

    // ------------------------------------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public PageResponse<OfertaResponse> listar(OfertaFiltro f, PaginaRequest pagina) {
        return PageResponse.of(repo.findAll(especificacion(f, false), PaginacionUtil.crear(pagina, SORT, "inicio,asc"))
                .map(this::aResponse));
    }

    /** Oferta publica: solo activas que no han terminado (PROGRAMADA o EN_CURSO). */
    @Transactional(readOnly = true)
    public PageResponse<OfertaResponse> listarPublico(OfertaFiltro f, PaginaRequest pagina) {
        return PageResponse.of(repo.findAll(especificacion(f, true), PaginacionUtil.crear(pagina, SORT, "inicio,asc"))
                .map(this::aResponse));
    }

    @Transactional(readOnly = true)
    public OfertaResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    // ------------------------------------------------------------------------------------------------ escritura

    @Transactional
    public OfertaResponse crear(OfertaRequest r) {
        LocalDateTime ahora = ahora();
        List<SesionOferta> sesiones = normalizar(r.sesiones());
        exigirFuturas(sesiones, ahora);
        Referencias ref = resolver(r, null);
        LocalDateTime inicio = sesiones.get(0).getInicio();
        if (repo.existsByNombreIgnoreCaseAndCategoriaIdAndInicio(r.nombre(), ref.categoria().getId(), inicio)) {
            throw duplicado(r.nombre(), inicio);
        }
        validarDocente(ref.docente(), sesiones, null);

        T o = nueva();
        copiar(o, r, ref, sesiones);
        o.setActivo(true);
        o = repo.save(o);
        registrarAgenda(o, ahora);
        return aResponse(o);
    }

    @Transactional
    public OfertaResponse actualizar(Long id, OfertaRequest r) {
        LocalDateTime ahora = ahora();
        T o = buscar(id);
        if (!o.isActivo()) {
            throw new ConflictoEstadoException("El " + etiqueta() + " esta desactivado: reactivalo antes de editarlo");
        }
        if (o.yaTermino(ahora)) {
            throw new ConflictoEstadoException("El " + etiqueta() + " ya finalizo: es solo lectura");
        }
        long vigentes = deps.getInscripciones().vigentes(tipo(), id);
        if (r.cupoMaximo() < vigentes) {
            throw new ConflictoEstadoException("El cupo (" + r.cupoMaximo() + ") no puede ser menor que las "
                    + vigentes + " inscripciones vigentes");
        }
        List<SesionOferta> sesiones = normalizar(r.sesiones());

        if (o.yaInicio(ahora)) {
            boolean restringido = !new ArrayList<>(o.getSesiones()).equals(sesiones)
                    || !o.getCategoria().getId().equals(r.categoriaId())
                    || !o.getModalidad().getId().equals(r.modalidadId())
                    || !o.getDocente().getId().equals(r.docenteId())
                    || !Objects.equals(o.getEspacio() == null ? null : o.getEspacio().getId(), r.espacioId());
            if (restringido) {
                throw new ConflictoEstadoException("El " + etiqueta() + " ya inicio: solo puedes cambiar nombre, "
                        + "descripcion, cupo y costo (las sesiones, el docente, la modalidad, la categoria y el espacio quedan fijos)");
            }
            if (o.getEspacio() != null && r.cupoMaximo() > o.getEspacio().getCapacidad()) {
                throw capacidad(r.cupoMaximo(), o.getEspacio());
            }
            validarNombreUnico(r.nombre(), o.getCategoria().getId(), o.getInicio(), id);
            o.setNombre(r.nombre());
            o.setDescripcion(r.descripcion());
            o.setCupoMaximo(r.cupoMaximo());
            o.setCosto(r.costo());
            return aResponse(repo.save(o));
        }

        exigirFuturas(sesiones, ahora);
        Referencias ref = resolver(r, o);
        validarNombreUnico(r.nombre(), ref.categoria().getId(), sesiones.get(0).getInicio(), id);
        validarDocente(ref.docente(), sesiones, id);
        // Reprogramar: se liberan las sesiones viejas ANTES de registrar las nuevas (si no, chocaria consigo mismo).
        deps.getAgenda().eliminar(tipoActividad(), id);
        copiar(o, r, ref, sesiones);
        o = repo.save(o);
        registrarAgenda(o, ahora);
        return aResponse(o);
    }

    @Transactional
    public void desactivar(Long id) {
        T o = buscar(id);
        if (!o.isActivo()) {
            throw new ConflictoEstadoException("El " + etiqueta() + " ya esta desactivado");
        }
        if (!o.yaTermino(ahora())) {
            deps.getInscripciones().cancelarPorBajaDeOferta(tipo(), id, o.getNombre());
            deps.getAgenda().eliminar(tipoActividad(), id);
        }
        o.setActivo(false);
        repo.save(o);
    }

    @Transactional
    public void activar(Long id) {
        LocalDateTime ahora = ahora();
        T o = buscar(id);
        if (o.isActivo()) {
            throw new ConflictoEstadoException("El " + etiqueta() + " ya esta activo");
        }
        if (!o.yaTermino(ahora)) {
            if (!o.getCategoria().isActivo() || !o.getModalidad().isActivo() || !o.getDocente().isActivo()) {
                throw new ConflictoEstadoException("No se puede reactivar: su categoria, modalidad o docente esta "
                        + "desactivado. Editalo despues de reactivarlo o reactiva primero ese catalogo.");
            }
            if (o.getEspacio() != null && !o.getEspacio().isActivo()) {
                throw new ConflictoEstadoException("No se puede reactivar: el espacio '" + o.getEspacio().getNombre()
                        + "' esta desactivado");
            }
            List<SesionOferta> futuras = o.getSesiones().stream().filter(s -> s.getFin().isAfter(ahora)).toList();
            validarDocente(o.getDocente(), futuras, id);
            registrarAgenda(o, ahora);
        }
        o.setActivo(true);
        repo.save(o);
    }

    public T buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de(titulo(), id));
    }

    // ------------------------------------------------------------------------------------------------ reglas

    /** Ordena, valida duracion y cruces internos. */
    List<SesionOferta> normalizar(List<SesionRequest> entrada) {
        List<SesionOferta> sesiones = entrada.stream()
                .map(s -> new SesionOferta(s.inicio().withSecond(0).withNano(0), s.fin().withSecond(0).withNano(0)))
                .sorted(Comparator.comparing(SesionOferta::getInicio))
                .toList();
        for (int i = 0; i < sesiones.size(); i++) {
            SesionOferta s = sesiones.get(i);
            String pref = sesiones.size() > 1 ? "Sesion " + (i + 1) + ": " : "";
            if (!s.getFin().isAfter(s.getInicio())) {
                throw new ReglaNegocioException(pref + "el fin debe ser posterior al inicio "
                        + "(si termina de madrugada, el fin es del dia siguiente)");
            }
            Duration d = Duration.between(s.getInicio(), s.getFin());
            if (d.compareTo(MIN_SESION) < 0 || d.compareTo(MAX_SESION) > 0) {
                throw new ReglaNegocioException(pref + "cada sesion debe durar entre 30 minutos y 12 horas");
            }
            if (i > 0 && s.getInicio().isBefore(sesiones.get(i - 1).getFin())) {
                throw new ReglaNegocioException("Dos sesiones se cruzan entre si: "
                        + AgendaService.FMT.format(sesiones.get(i - 1).getInicio()) + " y "
                        + AgendaService.FMT.format(s.getInicio()));
            }
        }
        return sesiones;
    }

    private void exigirFuturas(List<SesionOferta> sesiones, LocalDateTime ahora) {
        if (!sesiones.get(0).getInicio().isAfter(ahora)) {
            throw new ReglaNegocioException("Todas las sesiones deben ser futuras: la primera inicia el "
                    + AgendaService.FMT.format(sesiones.get(0).getInicio()));
        }
    }

    private record Referencias(Categoria categoria, Modalidad modalidad, Docente docente, Espacio espacio) {
    }

    /**
     * Carga y valida categoria, modalidad, docente y espacio. Un catalogo desactivado solo se acepta si la oferta
     * YA lo tenia (editar no obliga a cambiarlo), nunca como eleccion nueva.
     */
    private Referencias resolver(OfertaRequest r, T existente) {
        Categoria c = deps.getCategorias().findById(r.categoriaId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Categoria", r.categoriaId()));
        if (!c.isActivo() && (existente == null || !existente.getCategoria().getId().equals(c.getId()))) {
            throw new ReglaNegocioException("La categoria '" + c.getNombre() + "' esta desactivada");
        }
        Modalidad m = deps.getModalidades().findById(r.modalidadId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Modalidad", r.modalidadId()));
        if (!m.isActivo() && (existente == null || !existente.getModalidad().getId().equals(m.getId()))) {
            throw new ReglaNegocioException("La modalidad '" + m.getNombre() + "' esta desactivada");
        }
        Docente d = deps.getDocentes().findById(r.docenteId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Docente", r.docenteId()));
        if (!d.isActivo()) {
            throw new ReglaNegocioException("El docente '" + d.getNombre() + "' esta desactivado");
        }
        Espacio e = null;
        if (m.isRequiereEspacioFisico()) {
            if (r.espacioId() == null) {
                throw new ReglaNegocioException("La modalidad '" + m.getNombre() + "' requiere espacio fisico: "
                        + "asigna un aula o espacio (espacioId)");
            }
            e = deps.getEspacios().findById(r.espacioId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", r.espacioId()));
            if (!e.isActivo()) {
                throw new ReglaNegocioException("El espacio '" + e.getNombre() + "' esta desactivado");
            }
            if (r.cupoMaximo() > e.getCapacidad()) {
                throw capacidad(r.cupoMaximo(), e);
            }
        } else if (r.espacioId() != null) {
            throw new ReglaNegocioException("La modalidad '" + m.getNombre() + "' no requiere espacio fisico: un "
                    + etiqueta() + " virtual no puede tener un aula asignada");
        }
        return new Referencias(c, m, d, e);
    }

    /**
     * El docente no puede estar en dos ofertas vigentes con sesiones que se crucen (cursos Y diplomados).
     * Las ofertas desactivadas o ya terminadas NO lo ocupan.
     */
    private void validarDocente(Docente docente, List<SesionOferta> sesiones, Long excluirId) {
        deps.getDocentes().bloquear(docente.getId());
        LocalDateTime ahora = ahora();
        List<OfertaAcademica> otras = new ArrayList<>();
        otras.addAll(deps.getCursos().findByDocenteIdAndActivoTrueAndFinAfter(docente.getId(), ahora));
        otras.addAll(deps.getDiplomados().findByDocenteIdAndActivoTrueAndFinAfter(docente.getId(), ahora));
        for (OfertaAcademica otra : otras) {
            if (otra.getTipo() == tipo() && otra.getId().equals(excluirId)) {
                continue;
            }
            for (SesionOferta s : sesiones) {
                for (SesionOferta x : otra.getSesiones()) {
                    if (Horarios.seCruzan(s.getInicio(), s.getFin(), x.getInicio(), x.getFin())) {
                        throw new DocenteOcupadoException("El docente '" + docente.getNombre() + "' ya imparte el "
                                + otra.getTipo().name().toLowerCase() + " '" + otra.getNombre() + "' de "
                                + AgendaService.FMT.format(x.getInicio()) + " a " + AgendaService.FMT.format(x.getFin())
                                + ", que se cruza con la sesion de " + AgendaService.FMT.format(s.getInicio()));
                    }
                }
            }
        }
    }

    /** Una actividad por sesion futura; solo si la oferta usa espacio fisico. */
    private void registrarAgenda(T o, LocalDateTime ahora) {
        if (o.getEspacio() == null) {
            return;
        }
        List<SesionOferta> sesiones = o.getSesiones();
        List<NuevaActividad> actividades = new ArrayList<>();
        for (int i = 0; i < sesiones.size(); i++) {
            SesionOferta s = sesiones.get(i);
            if (!s.getFin().isAfter(ahora)) {
                continue;
            }
            String t = titulo() + ": " + o.getNombre() + (sesiones.size() > 1 ? " (sesion " + (i + 1) + "/"
                    + sesiones.size() + ")" : "");
            actividades.add(new NuevaActividad(tipoActividad(), o.getId(), t, s.getInicio(), s.getFin(), o.getEspacio(),
                    true, null, "Docente: " + o.getDocente().getNombre()));
        }
        deps.getAgenda().registrarTodas(actividades);
    }

    private void validarNombreUnico(String nombre, Long categoriaId, LocalDateTime inicio, Long id) {
        if (repo.existsByNombreIgnoreCaseAndCategoriaIdAndInicioAndIdNot(nombre, categoriaId, inicio, id)) {
            throw duplicado(nombre, inicio);
        }
    }

    private RegistroDuplicadoException duplicado(String nombre, LocalDateTime inicio) {
        return new RegistroDuplicadoException("Ya existe un " + etiqueta() + " '" + nombre + "' en esa categoria que "
                + "inicia el " + AgendaService.FMT.format(inicio) + ". Si es otra edicion, usa otra fecha de inicio.");
    }

    private ReglaNegocioException capacidad(int cupo, Espacio e) {
        return new ReglaNegocioException("El cupo maximo (" + cupo + ") supera la capacidad del espacio '"
                + e.getNombre() + "' (" + e.getCapacidad() + ")");
    }

    private void copiar(T o, OfertaRequest r, Referencias ref, List<SesionOferta> sesiones) {
        o.setNombre(r.nombre());
        o.setDescripcion(r.descripcion());
        o.setCategoria(ref.categoria());
        o.setModalidad(ref.modalidad());
        o.setDocente(ref.docente());
        o.setEspacio(ref.espacio());
        o.setCupoMaximo(r.cupoMaximo());
        o.setCosto(r.costo());
        o.getSesiones().clear();
        o.getSesiones().addAll(sesiones);
        o.setInicio(sesiones.get(0).getInicio());
        o.setFin(sesiones.stream().map(SesionOferta::getFin).max(Comparator.naturalOrder()).orElseThrow());
    }

    // ------------------------------------------------------------------------------------------------ mapeo

    private Specification<T> especificacion(OfertaFiltro f, boolean soloVigentes) {
        LocalDateTime ahora = ahora();
        String campoInscripcion = tipo() == TipoOferta.CURSO ? "curso" : "diplomado";
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (soloVigentes) {
                p.add(cb.isTrue(root.get("activo")));
                p.add(cb.greaterThan(root.get("fin"), ahora));
            }
            if (f != null) {
                String texto = Textos.vacioANull(f.q());
                if (texto != null) {
                    String like = "%" + texto.toLowerCase() + "%";
                    p.add(cb.or(cb.like(cb.lower(root.get("nombre")), like),
                            cb.like(cb.lower(cb.coalesce(root.<String>get("descripcion"), "")), like)));
                }
                if (f.categoriaId() != null) {
                    p.add(cb.equal(root.get("categoria").get("id"), f.categoriaId()));
                }
                if (f.modalidadId() != null) {
                    p.add(cb.equal(root.get("modalidad").get("id"), f.modalidadId()));
                }
                if (f.docenteId() != null) {
                    p.add(cb.equal(root.get("docente").get("id"), f.docenteId()));
                }
                if (f.espacioId() != null) {
                    p.add(cb.equal(root.get("espacio").get("id"), f.espacioId()));
                }
                if (f.desde() != null) {
                    p.add(cb.greaterThan(root.get("fin"), f.desde().atStartOfDay()));
                }
                if (f.hasta() != null) {
                    p.add(cb.lessThan(root.get("inicio"), f.hasta().plusDays(1).atStartOfDay()));
                }
                if (f.estado() != null) {
                    switch (f.estado()) {
                        case INACTIVA -> p.add(cb.isFalse(root.get("activo")));
                        case PROGRAMADA -> {
                            p.add(cb.isTrue(root.get("activo")));
                            p.add(cb.greaterThan(root.get("inicio"), ahora));
                        }
                        case EN_CURSO -> {
                            p.add(cb.isTrue(root.get("activo")));
                            p.add(cb.lessThanOrEqualTo(root.get("inicio"), ahora));
                            p.add(cb.greaterThan(root.get("fin"), ahora));
                        }
                        case FINALIZADA -> {
                            p.add(cb.isTrue(root.get("activo")));
                            p.add(cb.lessThanOrEqualTo(root.get("fin"), ahora));
                        }
                    }
                }
                if (Boolean.TRUE.equals(f.conCupo())) {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<Inscripcion> i = sq.from(Inscripcion.class);
                    sq.select(cb.count(i)).where(cb.equal(i.get(campoInscripcion).get("id"), root.get("id")),
                            i.get("estado").in(EstadoInscripcion.VIGENTES));
                    p.add(cb.lessThan(sq, root.get("cupoMaximo").as(Long.class)));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    protected OfertaResponse aResponse(T o) {
        LocalDateTime ahora = ahora();
        long inscritos = o.getId() == null ? 0 : deps.getInscripciones().vigentes(tipo(), o.getId());
        List<SesionOferta> ss = o.getSesiones();
        List<SesionResponse> sesiones = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < ss.size(); i++) {
            double h = Duration.between(ss.get(i).getInicio(), ss.get(i).getFin()).toMinutes() / 60.0;
            total += h;
            sesiones.add(new SesionResponse(i + 1, ss.get(i).getInicio(), ss.get(i).getFin(), h));
        }
        EstadoOferta estado = !o.isActivo() ? EstadoOferta.INACTIVA
                : o.yaTermino(ahora) ? EstadoOferta.FINALIZADA
                : o.yaInicio(ahora) ? EstadoOferta.EN_CURSO : EstadoOferta.PROGRAMADA;
        Espacio e = o.getEspacio();
        return new OfertaResponse(o.getId(), tipo(), o.getNombre(), o.getDescripcion(),
                o.getCategoria().getId(), o.getCategoria().getNombre(),
                o.getModalidad().getId(), o.getModalidad().getNombre(), o.getModalidad().isRequiereEspacioFisico(),
                o.getDocente().getId(), o.getDocente().getNombre(),
                e == null ? null : e.getId(), e == null ? null : e.getNombre(),
                o.getCupoMaximo(), inscritos, Math.max(0, o.getCupoMaximo() - inscritos), o.getCosto(),
                o.getInicio(), o.getFin(), total, estado, o.isActivo(), sesiones);
    }

    protected LocalDateTime ahora() {
        return LocalDateTime.now(deps.getClock());
    }
}
