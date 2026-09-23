package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ServicioCateringServiceImpl implements ServicioCateringService {

    private final ServicioCateringRepository servicioCateringRepository;

    public ServicioCateringServiceImpl(ServicioCateringRepository servicioCateringRepository) {
        this.servicioCateringRepository = servicioCateringRepository;
    }

    @Override
    @Transactional
    public ServicioCatering crear(ServicioCatering servicio) {
        validar(servicio);
        return servicioCateringRepository.save(servicio);
    }

    @Override
    public ServicioCatering buscarPorId(Long id) {
        return servicioCateringRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("ServicioCatering", id));
    }

    @Override
    public List<ServicioCatering> listar() {
        return servicioCateringRepository.findAll();
    }

    @Override
    @Transactional
    public ServicioCatering actualizar(Long id, ServicioCatering datos) {
        validar(datos);
        ServicioCatering existente = buscarPorId(id);
        existente.setTipo(datos.getTipo());
        existente.setNombre(datos.getNombre());
        existente.setPrecioUnitario(datos.getPrecioUnitario());
        return servicioCateringRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        servicioCateringRepository.delete(buscarPorId(id));
    }

    private void validar(ServicioCatering servicio) {
        if (servicio.getPrecioUnitario() != null && servicio.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException("El precio unitario no puede ser negativo");
        }
    }
}
