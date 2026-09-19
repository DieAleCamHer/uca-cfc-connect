package edu.udb.ucacfc.seguridad;

import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void registrar_hasheaLaContrasena_yGuardaElUsuario() {
        Usuario usuario = Usuario.builder().nombre("Ana").email("ana@udb.edu.sv").password("clave123").rol(Rol.RECEPCIONISTA).build();
        when(usuarioRepository.existsByEmail("ana@udb.edu.sv")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("HASH_BCRYPT");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.registrar(usuario);

        assertEquals("HASH_BCRYPT", resultado.getPassword());
        assertTrue(resultado.isActivo());
    }

    @Test
    void registrar_lanzaRegistroDuplicado_cuandoElEmailYaExiste() {
        Usuario usuario = Usuario.builder().nombre("Ana").email("ana@udb.edu.sv").password("clave123").rol(Rol.CLIENTE).build();
        when(usuarioRepository.existsByEmail("ana@udb.edu.sv")).thenReturn(true);

        assertThrows(RegistroDuplicadoException.class, () -> usuarioService.registrar(usuario));
    }

    @Test
    void buscarPorId_lanzaRecursoNoEncontrado_cuandoNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.buscarPorId(1L));
    }

    @Test
    void desactivar_dejaElUsuarioInactivo() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Ana").email("ana@udb.edu.sv").rol(Rol.CLIENTE).activo(true).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.desactivar(1L);

        assertFalse(usuario.isActivo());
    }
}
