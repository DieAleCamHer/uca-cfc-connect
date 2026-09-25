package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModalidadServiceImpl implements ModalidadService {

    private final ModalidadRepository modalidadRepository;

    public ModalidadServiceImpl(ModalidadRepository modalidadRepository) {
        this.modalidadRepository = modalidadRepository;
    }

    @Override
    @Transactional
    public Modalidad crear(Modalidad modalidad) {
        if (modalidadRepository.existsByNombreIgnoreCase(modalidad.getNombre())) {
            throw new RegistroDuplicadoException("Ya existe una modalidad con nombre: " + modalidad.getNombre());
        }
        return modalidadRepository.save(modalidad);
    }

    @Override
    public Modalidad buscarPorId(Long id) {
        return modalidadRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Modalidad", id));
    }

    @Override
    public List<Modalidad> listar() {
        return modalidadRepository.findAll();
    }

    @Override
    @Transactional
    public Modalidad actualizar(Long id, Modalidad datos) {
        Modalidad existente = buscarPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setRequiereEspacioFisico(datos.isRequiereEspacioFisico());
        return modalidadRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        modalidadRepository.delete(buscarPorId(id));
    }
}
