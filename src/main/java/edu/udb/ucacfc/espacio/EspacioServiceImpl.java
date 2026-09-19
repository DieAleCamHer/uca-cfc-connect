package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EspacioServiceImpl implements EspacioService {

    private final EspacioRepository espacioRepository;

    public EspacioServiceImpl(EspacioRepository espacioRepository) {
        this.espacioRepository = espacioRepository;
    }

    @Override
    @Transactional
    public Espacio crear(Espacio espacio) {
        validar(espacio);
        return espacioRepository.save(espacio);
    }

    @Override
    public Espacio buscarPorId(Long id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Espacio", id));
    }

    @Override
    public List<Espacio> listar() {
        return espacioRepository.findAll();
    }

    @Override
    public Page<Espacio> listarPaginado(Pageable pageable) {
        return espacioRepository.findAll(pageable);
    }

    @Override
    public List<Espacio> listarDisponibles() {
        return espacioRepository.findByDisponibleTrue();
    }

    @Override
    @Transactional
    public Espacio actualizar(Long id, Espacio datos) {
        validar(datos);
        Espacio existente = buscarPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setTipo(datos.getTipo());
        existente.setCapacidad(datos.getCapacidad());
        existente.setPrecio(datos.getPrecio());
        existente.setEquipamiento(datos.getEquipamiento());
        existente.setDisponible(datos.isDisponible());
        return espacioRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Espacio existente = buscarPorId(id);
        existente.setDisponible(false);
        espacioRepository.save(existente);
    }

    private void validar(Espacio espacio) {
        if (espacio.getCapacidad() != null && espacio.getCapacidad() <= 0) {
            throw new OperacionInvalidaException("La capacidad debe ser mayor a cero");
        }
        if (espacio.getPrecio() != null && espacio.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException("El precio no puede ser negativo");
        }
    }
}
