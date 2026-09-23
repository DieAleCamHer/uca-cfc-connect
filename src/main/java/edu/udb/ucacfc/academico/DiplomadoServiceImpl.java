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
public class DiplomadoServiceImpl implements DiplomadoService {

    private final DiplomadoRepository diplomadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ModalidadRepository modalidadRepository;
    private final DocenteRepository docenteRepository;
    private final EspacioRepository espacioRepository;
    private final AgendaService agendaService;

    public DiplomadoServiceImpl(DiplomadoRepository diplomadoRepository,
                                 CategoriaRepository categoriaRepository,
                                 ModalidadRepository modalidadRepository,
                                 DocenteRepository docenteRepository,
                                 EspacioRepository espacioRepository,
                                 AgendaService agendaService) {
        this.diplomadoRepository = diplomadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.modalidadRepository = modalidadRepository;
        this.docenteRepository = docenteRepository;
        this.espacioRepository = espacioRepository;
        this.agendaService = agendaService;
    }

    @Override
    @Transactional
    public Diplomado crear(Diplomado diplomado) {
        validarDatos(diplomado);
        categoriaRepository.findById(diplomado.getCategoria().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Categoria", diplomado.getCategoria().getId()));
        Modalidad modalidad = modalidadRepository.findById(diplomado.getModalidad().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Modalidad", diplomado.getModalidad().getId()));
        docenteRepository.findById(diplomado.getDocente().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Docente", diplomado.getDocente().getId()));

        Espacio espacio = resolverYValidarEspacio(diplomado, modalidad);
        diplomado.setEspacio(espacio);
        diplomado.setActivo(true);
        Diplomado guardado = diplomadoRepository.save(diplomado);

        if (espacio != null) {
            registrarEnAgenda(guardado, espacio);
        }
        return guardado;
    }

    @Override
    public Diplomado buscarPorId(Long id) {
        return diplomadoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Diplomado", id));
    }

    @Override
    public List<Diplomado> listar() {
        return diplomadoRepository.findAll();
    }

    @Override
    public List<Diplomado> listarActivos() {
        return diplomadoRepository.findByActivoTrue();
    }

    @Override
    @Transactional
    public Diplomado actualizar(Long id, Diplomado datos) {
        validarDatos(datos);
        Diplomado existente = buscarPorId(id);
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
        Diplomado guardado = diplomadoRepository.save(existente);

        if (espacio != null) {
            registrarEnAgenda(guardado, espacio);
        }
        return guardado;
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Diplomado existente = buscarPorId(id);
        existente.setActivo(false);
        diplomadoRepository.save(existente);
    }

    private Espacio resolverYValidarEspacio(Diplomado diplomado, Modalidad modalidad) {
        Long espacioId = diplomado.getEspacio() != null ? diplomado.getEspacio().getId() : null;

        if (!modalidad.isRequiereEspacioFisico()) {
            if (espacioId != null) {
                throw new OperacionInvalidaException(
                        "La modalidad '" + modalidad.getNombre() + "' no requiere espacio fisico; "
                                + "no se puede asignar un aula a un diplomado virtual.");
            }
            return null;
        }

        if (espacioId == null) {
            throw new OperacionInvalidaException(
                    "La modalidad '" + modalidad.getNombre() + "' requiere espacio fisico; "
                            + "debes asignar un aula/espacio a este diplomado.");
        }
        return espacioRepository.findById(espacioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", espacioId));
    }

    private void registrarEnAgenda(Diplomado diplomado, Espacio espacio) {
        agendaService.registrarActividad(
                TipoActividad.DIPLOMADO,
                diplomado.getId(),
                "Diplomado: " + diplomado.getNombre(),
                diplomado.getFechaInicio().atTime(diplomado.getHoraInicio()),
                diplomado.getFechaFin().atTime(diplomado.getHoraFin()),
                espacio
        );
    }

    private void validarDatos(Diplomado diplomado) {
        if (diplomado.getFechaInicio() != null && diplomado.getFechaFin() != null
                && diplomado.getFechaFin().isBefore(diplomado.getFechaInicio())) {
            throw new OperacionInvalidaException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        if (diplomado.getHoraInicio() != null && diplomado.getHoraFin() != null
                && !diplomado.getHoraFin().isAfter(diplomado.getHoraInicio())) {
            throw new OperacionInvalidaException("La hora fin debe ser posterior a la hora inicio");
        }
        if (diplomado.getCupoMaximo() != null && diplomado.getCupoMaximo() <= 0) {
            throw new OperacionInvalidaException("El cupo maximo debe ser mayor a cero");
        }
        if (diplomado.getCosto() != null && diplomado.getCosto().compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException("El costo no puede ser negativo");
        }
    }
}
