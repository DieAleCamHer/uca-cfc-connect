package edu.udb.ucacfc.cotizacion;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CotizacionServiceImpl implements CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;

    public CotizacionServiceImpl(CotizacionRepository cotizacionRepository, ClienteRepository clienteRepository) {
        this.cotizacionRepository = cotizacionRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public Cotizacion crear(Cotizacion cotizacion) {
        if (cotizacion.getTotal() != null && cotizacion.getTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException("El total no puede ser negativo");
        }
        Cliente cliente = clienteRepository.findById(cotizacion.getCliente().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", cotizacion.getCliente().getId()));

        cotizacion.setCliente(cliente);
        cotizacion.setEstado(EstadoCotizacion.PENDIENTE);
        cotizacion.setFechaSolicitud(LocalDate.now());
        return cotizacionRepository.save(cotizacion);
    }

    @Override
    public Cotizacion buscarPorId(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cotizacion", id));
    }

    @Override
    public List<Cotizacion> listar() {
        return cotizacionRepository.findAll();
    }

    @Override
    public List<Cotizacion> listarPorCliente(Long clienteId) {
        return cotizacionRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional
    public Cotizacion cambiarEstado(Long id, EstadoCotizacion nuevoEstado) {
        Cotizacion existente = buscarPorId(id);
        existente.setEstado(nuevoEstado);
        return cotizacionRepository.save(existente);
    }
}
