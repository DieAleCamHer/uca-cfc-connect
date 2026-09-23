package edu.udb.ucacfc.inscripcion;

import edu.udb.ucacfc.academico.Curso;
import edu.udb.ucacfc.academico.CursoRepository;
import edu.udb.ucacfc.academico.Diplomado;
import edu.udb.ucacfc.academico.DiplomadoRepository;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.shared.exception.CupoExcedidoException;
import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InscripcionServiceImpl implements InscripcionService {

    // Estados que "ocupan" un cupo (una inscripcion cancelada libera el cupo)
    private static final List<EstadoInscripcion> ESTADOS_QUE_OCUPAN_CUPO =
            List.of(EstadoInscripcion.PENDIENTE, EstadoInscripcion.CONFIRMADA);

    private final InscripcionRepository inscripcionRepository;
    private final ClienteRepository clienteRepository;
    private final CursoRepository cursoRepository;
    private final DiplomadoRepository diplomadoRepository;

    public InscripcionServiceImpl(InscripcionRepository inscripcionRepository,
                                   ClienteRepository clienteRepository,
                                   CursoRepository cursoRepository,
                                   DiplomadoRepository diplomadoRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.clienteRepository = clienteRepository;
        this.cursoRepository = cursoRepository;
        this.diplomadoRepository = diplomadoRepository;
    }

    @Override
    @Transactional
    public Inscripcion inscribir(Long clienteId, Long cursoId, Long diplomadoId) {
        boolean tieneCurso = cursoId != null;
        boolean tieneDiplomado = diplomadoId != null;
        if (tieneCurso == tieneDiplomado) {
            // true==true (ambos) o false==false (ninguno): las dos son invalidas
            throw new OperacionInvalidaException(
                    "Debes indicar exactamente un curso O un diplomado, no ambos ni ninguno");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", clienteId));

        Inscripcion.InscripcionBuilder builder = Inscripcion.builder()
                .cliente(cliente)
                .fecha(LocalDate.now())
                .estado(EstadoInscripcion.PENDIENTE);

        if (tieneCurso) {
            Curso curso = cursoRepository.findById(cursoId)
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Curso", cursoId));
            long inscritos = inscripcionRepository.countByCursoIdAndEstadoIn(cursoId, ESTADOS_QUE_OCUPAN_CUPO);
            if (inscritos >= curso.getCupoMaximo()) {
                throw new CupoExcedidoException("El curso '" + curso.getNombre() + "' ya alcanzo su cupo maximo");
            }
            builder.curso(curso);
        } else {
            Diplomado diplomado = diplomadoRepository.findById(diplomadoId)
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Diplomado", diplomadoId));
            long inscritos = inscripcionRepository.countByDiplomadoIdAndEstadoIn(diplomadoId, ESTADOS_QUE_OCUPAN_CUPO);
            if (inscritos >= diplomado.getCupoMaximo()) {
                throw new CupoExcedidoException("El diplomado '" + diplomado.getNombre() + "' ya alcanzo su cupo maximo");
            }
            builder.diplomado(diplomado);
        }

        return inscripcionRepository.save(builder.build());
    }

    @Override
    public Inscripcion buscarPorId(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Inscripcion", id));
    }

    @Override
    public List<Inscripcion> listar() {
        return inscripcionRepository.findAll();
    }

    @Override
    public List<Inscripcion> listarPorCliente(Long clienteId) {
        return inscripcionRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional
    public Inscripcion cambiarEstado(Long id, EstadoInscripcion nuevoEstado) {
        Inscripcion existente = buscarPorId(id);
        existente.setEstado(nuevoEstado);
        return inscripcionRepository.save(existente);
    }
}
