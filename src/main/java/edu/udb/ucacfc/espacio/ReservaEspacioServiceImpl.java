package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.agenda.AgendaService;
import edu.udb.ucacfc.agenda.TipoActividad;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.shared.exception.EspacioOcupadoException;
import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaEspacioServiceImpl implements ReservaEspacioService {

    private final ReservaEspacioRepository reservaEspacioRepository;
    private final EspacioRepository espacioRepository;
    private final ClienteRepository clienteRepository;
    private final AgendaService agendaService;

    public ReservaEspacioServiceImpl(ReservaEspacioRepository reservaEspacioRepository,
                                      EspacioRepository espacioRepository,
                                      ClienteRepository clienteRepository,
                                      AgendaService agendaService) {
        this.reservaEspacioRepository = reservaEspacioRepository;
        this.espacioRepository = espacioRepository;
        this.clienteRepository = clienteRepository;
        this.agendaService = agendaService;
    }

    @Override
    @Transactional
    public ReservaEspacio reservar(Long espacioId, Long clienteId, LocalDateTime inicio,
                                    LocalDateTime fin, String motivo) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new OperacionInvalidaException("La fecha/hora fin debe ser posterior a la fecha/hora inicio");
        }

        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", espacioId));
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", clienteId));

        // 1) Validacion propia del modulo de espacios (rapida, una sola tabla)
        List<ReservaEspacio> solapadas = reservaEspacioRepository.findSolapadas(espacioId, inicio, fin);
        if (!solapadas.isEmpty()) {
            throw new EspacioOcupadoException(
                    "El espacio '" + espacio.getNombre() + "' ya tiene una reserva entre "
                            + inicio + " y " + fin);
        }

        ReservaEspacio reserva = ReservaEspacio.builder()
                .espacio(espacio)
                .cliente(cliente)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(fin)
                .motivo(motivo)
                .estado(EstadoReserva.RESERVADO)
                .build();
        reserva = reservaEspacioRepository.save(reserva);

        // 2) Se refleja tambien en la agenda institucional (valida otra vez
        // contra TODOS los tipos de actividad, no solo alquileres, por si
        // ese mismo espacio ya tiene un curso presencial programado).
        agendaService.registrarActividad(
                TipoActividad.ALQUILER,
                reserva.getId(),
                "Alquiler: " + espacio.getNombre() + " - " + cliente.getNombre(),
                inicio, fin, espacio
        );

        return reserva;
    }

    @Override
    public ReservaEspacio buscarPorId(Long id) {
        return reservaEspacioRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("ReservaEspacio", id));
    }

    @Override
    public List<ReservaEspacio> listar() {
        return reservaEspacioRepository.findAll();
    }

    @Override
    public Page<ReservaEspacio> listarPaginado(Pageable pageable) {
        return reservaEspacioRepository.findAll(pageable);
    }

    @Override
    public List<ReservaEspacio> listarPorCliente(Long clienteId) {
        return reservaEspacioRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional
    public void cancelar(Long id) {
        ReservaEspacio reserva = buscarPorId(id);
        reserva.setEstado(EstadoReserva.CANCELADO);
        reservaEspacioRepository.save(reserva);
        // Al cancelar, se libera el horario tambien en la agenda general.
        agendaService.eliminarPorReferencia(TipoActividad.ALQUILER, reserva.getId());
    }
}
