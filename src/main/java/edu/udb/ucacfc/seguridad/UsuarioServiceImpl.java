package edu.udb.ucacfc.seguridad;

import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new RegistroDuplicadoException("Ya existe un usuario con el email: " + usuario.getEmail());
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con email " + email + " no fue encontrado"));
    }

    @Override
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Override
    public Page<Usuario> listarPaginado(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Usuario actualizar(Long id, Usuario datos) {
        Usuario existente = buscarPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setRol(datos.getRol());
        return usuarioRepository.save(existente);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Usuario existente = buscarPorId(id);
        existente.setActivo(false);
        usuarioRepository.save(existente);
    }
}
