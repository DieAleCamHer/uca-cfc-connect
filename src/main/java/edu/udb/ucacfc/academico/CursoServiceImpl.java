package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.agenda.TipoActividad;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ModalidadRepository modalidadRepository;
    private final DocenteRepository docenteRepository;
    private final EspacioRepository espacioRepository;
    private final AgendaService agendaService;

    public CursoServiceImpl(CursoRepository cursoRepository,
                             CategoriaRepository categoriaRepository,
                             ModalidadRepository modalidadRepository,
                             DocenteRepository docenteRepository,
                             EspacioRepository espacioRepository,
                             AgendaService agendaService) {
        this.cursoRepository = cursoRepository;
        this.categoriaRepository = categoriaRepository;
        this.modalidadRepository = modalidadRepository;
        this.docenteRepository = docenteRepository;
        this.espacioRepository = espacioRepository;
        this.agendaService = agendaService;
    }

    @Override
    @Transactional
    public Curso crear(Curso curso) {
        validarDatos(curso);
        // Verifica que las FK realmente existan (mejor un 404/400 claro
        // que un error de integridad referencial de MySQL).
        categoriaRepository.findById(curso.getCategoria().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Categoria", curso.getCategoria().getId()));
        Modalidad modalidad = modalidadRepository.findById(curso.getModalidad().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Modalidad", curso.getModalidad().getId()));
        docenteRepository.findById(curso.getDocente().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Docente", curso.getDocente().getId()));

        Espacio espacio = resolverYValidarEspacio(curso, modalidad);
        curso.setEspacio(espacio);
        curso.setActivo(true);
        Curso guardado = cursoRepository.save(curso);

        if (espacio != null) {
            registrarEnAgenda(guardado, espacio);
        }
        return guardado;
    }

    @Override
    public Curso buscarPorId(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Curso", id));
    }

    @Override
    public List<Curso> listar() {
        return cursoRepository.findAll();
    }

    @Override
    public List<Curso> listarActivos() {
        return cursoRepository.findByActivoTrue();
    }

    @Override
    @Transactional
    public Curso actualizar(Long id, Curso datos) {
        validarDatos(datos);
        Curso existente = buscarPorId(id);
        Modalidad modalidad = modalidadRepository.findById(datos.getModalidad().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Modalidad", datos.getModalidad().getId()));
        Espacio espacio = resolverYValidarEspacio(datos, modalidad);

        existente.setNombre(datos.getNombre());
        existente.setCategoria(datos.getCategoria());
        existente.setModalidad(datos.getModalidad());
        existente.setDocente(datos.getDocente());
        existente.setEspacio(espacio);
        existente.setCupoMaximo(datos.getCupoMaximo());
        existente.setFechaInicio(datos.getFechaInicio());
        existente.setFechaFin(datos.getFechaFin());
        existente.setHoraInicio(datos.getHoraInicio());
        existente.setHoraFin(datos.getHoraFin());
        existente.setCosto(datos.getCosto());
        Curso guardado = cursoRepository.save(existente);

        if (espacio != null) {
            registrarEnAgenda(guardado, espacio);
        }
        return guardado;
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        // Soft delete: un curso con inscritos no deberia desaparecer de la
        // BD, solo dejar de ofertarse.
        Curso existente = buscarPorId(id);
        existente.setActivo(false);
        cursoRepository.save(existente);
    }

    /**
     * Regla de negocio exigida por el lineamiento: un curso de modalidad
     * VIRTUAL (o cualquier modalidad con requiereEspacioFisico=false) NO
     * puede tener un aula/espacio fisico asignado. Si la modalidad SI lo
     * requiere y no se asigno ninguno, tambien se rechaza (no tendria
     * sentido un curso presencial sin lugar donde impartirse).
     */
    private Espacio resolverYValidarEspacio(Curso curso, Modalidad modalidad) {
        Long espacioId = curso.getEspacio() != null ? curso.getEspacio().getId() : null;

        if (!modalidad.isRequiereEspacioFisico()) {
            if (espacioId != null) {
                throw new OperacionInvalidaException(
                        "La modalidad '" + modalidad.getNombre() + "' no requiere espacio fisico; "
                                + "no se puede asignar un aula a un curso virtual.");
            }
            return null;
        }

        if (espacioId == null) {
            throw new OperacionInvalidaException(
                    "La modalidad '" + modalidad.getNombre() + "' requiere espacio fisico; "
                            + "debes asignar un aula/espacio a este curso.");
        }
        return espacioRepository.findById(espacioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", espacioId));
    }

    /**
     * Registra el curso en la agenda institucional ocupando el espacio
     * durante TODO su rango de fechas/horas. Si ya hay otra actividad
     * (curso, alquiler, evento, etc.) en ese mismo espacio y horario,
     * AgendaService lanza EspacioOcupadoException.
     */
    private void registrarEnAgenda(Curso curso, Espacio espacio) {
        agendaService.registrarActividad(
                TipoActividad.CURSO,
                curso.getId(),
                "Curso: " + curso.getNombre(),
                curso.getFechaInicio().atTime(curso.getHoraInicio()),
                curso.getFechaFin().atTime(curso.getHoraFin()),
                espacio
        );
    }

    private void validarDatos(Curso curso) {
        if (curso.getFechaInicio() != null && curso.getFechaFin() != null
                && curso.getFechaFin().isBefore(curso.getFechaInicio())) {
            throw new OperacionInvalidaException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        if (curso.getHoraInicio() != null && curso.getHoraFin() != null
                && !curso.getHoraFin().isAfter(curso.getHoraInicio())) {
            throw new OperacionInvalidaException("La hora fin debe ser posterior a la hora inicio");
        }
        if (curso.getCupoMaximo() != null && curso.getCupoMaximo() <= 0) {
            throw new OperacionInvalidaException("El cupo maximo debe ser mayor a cero");
        }
        if (curso.getCosto() != null && curso.getCosto().compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException("El costo no puede ser negativo");
        }
    }
}
