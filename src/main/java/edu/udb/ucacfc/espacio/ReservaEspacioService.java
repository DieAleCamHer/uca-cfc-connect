package edu.udb.ucacfc.espacio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaEspacioService {

    /**
     * Crea la reserva SOLO si el espacio esta libre en ese rango de horario.
     * Lanza EspacioOcupadoException si hay cruce.
     */
    ReservaEspacio reservar(Long espacioId, Long clienteId, LocalDateTime inicio,
                             LocalDateTime fin, String motivo);

    ReservaEspacio buscarPorId(Long id);

    List<ReservaEspacio> listar();

    Page<ReservaEspacio> listarPaginado(Pageable pageable);

    List<ReservaEspacio> listarPorCliente(Long clienteId);

    void cancelar(Long id);
}
