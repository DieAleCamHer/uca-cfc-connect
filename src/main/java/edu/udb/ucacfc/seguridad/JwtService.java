package edu.udb.ucacfc.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Genera y valida los JWT que viajan en el header "Authorization: Bearer ...".
 * Usa la libreria jjwt (ya esta en el pom.xml desde el Bloque 1).
 */
@Service
public class JwtService {

    // La clave y el tiempo de expiracion viven en application.properties,
    // no en el codigo (asi cada quien puede tener su propio secreto local).
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generarToken(UserDetails userDetails) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clavePrivada())
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Un token es valido si: el usuario del token coincide con el usuario
     * que se esta autenticando Y el token no esta expirado.
     *
     * jjwt lanza ExpiredJwtException al intentar PARSEAR un token ya vencido
     * (no solo al leer su fecha), asi que ese caso tambien cuenta como
     * "no valido" en vez de dejar que la excepcion se propague.
     */
    public boolean esValido(String token, UserDetails userDetails) {
        try {
            String username = extraerUsername(token);
            return username.equals(userDetails.getUsername()) && !estaExpirado(token);
        } catch (ExpiredJwtException ex) {
            return false;
        }
    }

    private boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(clavePrivada())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private SecretKey clavePrivada() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
