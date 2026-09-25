package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocenteServiceImpl implements DocenteService {

    private final DocenteRepository docenteRepository;

    public DocenteServiceImpl(DocenteRepository docenteRepository) {
        this.docenteRepository = docenteRepository;
    }

    @Override
    @Transactional
    public Docente crear(Docente docente) {
        return docenteRepository.save(docente);
    }

    @Override
    public Docente buscarPorId(Long id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Docente", id));
    }

    @Override
    public List<Docente> listar() {
        return docenteRepository.findAll();
    }

    @Override
    @Transactional
    public Docente actualizar(Long id, Docente datos) {
        Docente existente = buscarPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setEspecialidad(datos.getEspecialidad());
        existente.setCorreo(datos.getCorreo());
        existente.setTelefono(datos.getTelefono());
        return docenteRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        docenteRepository.delete(buscarPorId(id));
    }
}
