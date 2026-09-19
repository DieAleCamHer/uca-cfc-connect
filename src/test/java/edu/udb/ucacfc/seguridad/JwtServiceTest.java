package edu.udb.ucacfc.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtService no necesita Mockito: no tiene dependencias (repositorios, etc.),
 * asi que se prueba instanciandolo directo e inyectando "secret"/"expiration-ms"
 * por reflection (ReflectionTestUtils), ya que esos valores normalmente
 * los pone Spring desde application.properties via @Value.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "clave-de-prueba-para-tests-unitarios-32-caracteres-o-mas");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 1000L * 60 * 60); // 1 hora

        usuario = new User("ana@udb.edu.sv", "hash", List.of());
    }

    @Test
    void generarToken_creaUnTokenNoVacio() {
        String token = jwtService.generarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extraerUsername_devuelveElEmailUsadoParaGenerarElToken() {
        String token = jwtService.generarToken(usuario);

        assertEquals("ana@udb.edu.sv", jwtService.extraerUsername(token));
    }

    @Test
    void esValido_devuelveTrue_cuandoElTokenCorrespondeAlUsuario() {
        String token = jwtService.generarToken(usuario);

        assertTrue(jwtService.esValido(token, usuario));
    }

    @Test
    void esValido_devuelveFalse_cuandoElTokenEsDeOtroUsuario() {
        String token = jwtService.generarToken(usuario);
        UserDetails otroUsuario = new User("otro@udb.edu.sv", "hash", List.of());

        assertFalse(jwtService.esValido(token, otroUsuario));
    }

    @Test
    void esValido_devuelveFalse_cuandoElTokenYaExpiro() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // ya vencido al generarlo
        String token = jwtService.generarToken(usuario);

        assertFalse(jwtService.esValido(token, usuario));
    }
}
