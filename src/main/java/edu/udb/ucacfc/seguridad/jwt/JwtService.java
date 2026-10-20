package edu.udb.ucacfc.seguridad.jwt;

import edu.udb.ucacfc.seguridad.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * Fabrica y verifica los JWT (jjwt 0.12).
 *  - ACCESS (corto, 30 min): va en Authorization: Bearer en cada peticion.
 *  - REFRESH (largo, 7 dias): solo sirve en POST /api/auth/refresh para pedir un access nuevo.
 * Claims: sub = email, uid = id, rol (informativo para el frontend), ver = tokenVersion, type = access|refresh.
 * La autorizacion NO usa el claim rol: el filtro relee el usuario de la BD en cada peticion.
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final Duration duracionAccess;
    private final Duration duracionRefresh;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.access-minutes}") long accessMinutes,
                      @Value("${app.jwt.refresh-days}") long refreshDays) {
        if (secreto.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 64 bytes para HS512");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracionAccess = Duration.ofMinutes(accessMinutes);
        this.duracionRefresh = Duration.ofDays(refreshDays);
    }

    public String generarAccess(Usuario u) {
        return generar(u, TIPO_ACCESS, duracionAccess);
    }

    public String generarRefresh(Usuario u) {
        return generar(u, TIPO_REFRESH, duracionRefresh);
    }

    public long segundosAccess() {
        return duracionAccess.toSeconds();
    }

    public long segundosRefresh() {
        return duracionRefresh.toSeconds();
    }

    private String generar(Usuario u, String tipo, Duration duracion) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("uid", u.getId())
                .claim("rol", u.getRol().name())
                .claim("ver", u.getTokenVersion())
                .claim("type", tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracion.toMillis()))
                .signWith(clave, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Verifica firma y expiracion. Lanza ExpiredJwtException (vencido) o JwtException/IllegalArgumentException
     * (alterado o basura); quien llama decide el mensaje.
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }

    public boolean esRefresh(Claims claims) {
        return TIPO_REFRESH.equals(claims.get("type", String.class));
    }

    public boolean esAccess(Claims claims) {
        return TIPO_ACCESS.equals(claims.get("type", String.class));
    }

    public boolean coincideVersion(Claims claims, Usuario u) {
        Integer ver = claims.get("ver", Integer.class);
        return ver != null && ver == u.getTokenVersion();
    }
}
