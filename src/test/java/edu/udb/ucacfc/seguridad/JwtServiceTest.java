package edu.udb.ucacfc.seguridad;

import edu.udb.ucacfc.TestData;
import edu.udb.ucacfc.seguridad.jwt.JwtService;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JwtService no usa base de datos: se prueba directamente, sin mocks. */
class JwtServiceTest {

    static final String SECRETO = "secreto-de-pruebas-con-al-menos-sesenta-y-cuatro-bytes-0123456789-abcdef";
    final JwtService jwt = new JwtService(SECRETO, 30, 7);

    private Usuario ana() {
        Usuario u = TestData.usuario(5, Rol.CLIENTE);
        u.setEmail("ana@correo.com");
        return u;
    }

    @Test
    @DisplayName("El access token lleva correo, id, rol, version y tipo 'access'")
    void accessTokenConClaims() {
        Claims c = jwt.parse(jwt.generarAccess(ana()));
        assertThat(c.getSubject()).isEqualTo("ana@correo.com");
        assertThat(c.get("uid", Long.class)).isEqualTo(5L);
        assertThat(c.get("rol", String.class)).isEqualTo("CLIENTE");
        assertThat(jwt.esAccess(c)).isTrue();
        assertThat(jwt.esRefresh(c)).isFalse();
        assertThat(jwt.segundosAccess()).isEqualTo(1800);
    }

    @Test
    @DisplayName("El refresh token es de tipo 'refresh' (el filtro lo rechaza como Bearer)")
    void refreshTokenTipo() {
        Claims c = jwt.parse(jwt.generarRefresh(ana()));
        assertThat(jwt.esRefresh(c)).isTrue();
        assertThat(jwt.esAccess(c)).isFalse();
    }

    @Test
    @DisplayName("Token alterado -> la firma no coincide y se rechaza")
    void tokenAlterado() {
        String t = jwt.generarAccess(ana());
        String alterado = t.substring(0, t.length() - 3) + (t.endsWith("aaa") ? "bbb" : "aaa");
        assertThatThrownBy(() -> jwt.parse(alterado)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token firmado con otro secreto se rechaza")
    void otroSecreto() {
        JwtService otro = new JwtService(SECRETO.replace("pruebas", "atacante"), 30, 7);
        assertThatThrownBy(() -> jwt.parse(otro.generarAccess(ana()))).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token vencido -> ExpiredJwtException (el filtro responde 401 TOKEN_EXPIRED)")
    void tokenVencido() throws InterruptedException {
        JwtService instantaneo = new JwtService(SECRETO, 0, 7);
        String t = instantaneo.generarAccess(ana());
        Thread.sleep(1100);
        assertThatThrownBy(() -> jwt.parse(t)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Logout / cambio de contrasena: al subir tokenVersion el token viejo queda revocado")
    void revocacionPorVersion() {
        Usuario u = ana();
        Claims viejo = jwt.parse(jwt.generarAccess(u));
        assertThat(jwt.coincideVersion(viejo, u)).isTrue();
        u.revocarTokens();
        assertThat(jwt.coincideVersion(viejo, u)).isFalse();
    }

    @Test
    @DisplayName("Un secreto de menos de 64 bytes no se acepta al arrancar")
    void secretoCorto() {
        assertThatThrownBy(() -> new JwtService("corto", 30, 7)).isInstanceOf(IllegalStateException.class);
    }
}
