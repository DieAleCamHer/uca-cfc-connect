package edu.udb.ucacfc.seguridad.auth;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.cliente.ClienteValidador;
import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.seguridad.auth.dto.AuthResponse;
import edu.udb.ucacfc.seguridad.auth.dto.CambiarPasswordRequest;
import edu.udb.ucacfc.seguridad.auth.dto.LoginRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RecuperacionResponse;
import edu.udb.ucacfc.seguridad.auth.dto.RecuperarRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RefreshRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RegistroRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RestablecerRequest;
import edu.udb.ucacfc.seguridad.jwt.JwtService;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.seguridad.usuario.UsuarioRepository;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioResponse;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import edu.udb.ucacfc.shared.exception.TokenInvalidoException;
import edu.udb.ucacfc.shared.web.MensajeResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String MSG_RECUPERACION =
            "Si el correo esta registrado y activo, recibiras un enlace para restablecer tu contrasena.";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteValidador clienteValidador;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenFactory tokenFactory;
    private final Clock clock;
    private final long minutosRecuperacion;
    private final boolean mostrarToken;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                           ClienteRepository clienteRepository, ClienteValidador clienteValidador,
                           PasswordEncoder passwordEncoder, JwtService jwtService, TokenFactory tokenFactory,
                           Clock clock,
                           @Value("${app.recuperacion.expiracion-minutos}") long minutosRecuperacion,
                           @Value("${app.recuperacion.mostrar-token}") boolean mostrarToken) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.clienteValidador = clienteValidador;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenFactory = tokenFactory;
        this.clock = clock;
        this.minutosRecuperacion = minutosRecuperacion;
        this.mostrarToken = mostrarToken;
    }

    /**
     * authenticate() -> DaoAuthenticationProvider: busca por correo y compara BCrypt.
     * Credenciales malas -> BadCredentialsException (401); cuenta desactivada -> DisabledException (401).
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Usuario usuario = (Usuario) auth.getPrincipal();
        usuario.setUltimoAcceso(LocalDateTime.now(clock));
        usuarioRepository.save(usuario);
        return tokenFactory.emitir(usuario);
    }

    /** Cuenta CLIENTE + ficha de cliente en una sola transaccion: si algo falla no queda ninguna de las dos. */
    @Override
    @Transactional
    public AuthResponse registrar(RegistroRequest r) {
        if (usuarioRepository.existsByEmailIgnoreCase(r.email())) {
            throw new RegistroDuplicadoException("Ya existe una cuenta con el correo " + r.email());
        }
        clienteValidador.validarTipo(r.tipo(), r.duiNit(), r.empresa());
        clienteValidador.validarUnicidad(r.duiNit(), r.email(), r.telefono(), null);

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(r.nombre())
                .email(r.email().toLowerCase())
                .password(passwordEncoder.encode(r.password()))
                .rol(Rol.CLIENTE)
                .ultimoAcceso(LocalDateTime.now(clock))
                .build());

        clienteRepository.save(Cliente.builder()
                .tipo(r.tipo())
                .duiNit(r.duiNit())
                .nombre(r.nombre())
                .empresa(r.tipo() == TipoCliente.EMPRESA ? r.empresa() : null)
                .correo(r.email().toLowerCase())
                .telefono(r.telefono())
                .direccion(r.direccion())
                .usuario(usuario)
                .build());
        return tokenFactory.emitir(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (ExpiredJwtException e) {
            throw new TokenInvalidoException("REFRESH_EXPIRED", "Tu sesion expiro. Inicia sesion de nuevo");
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException("REFRESH_INVALID", "El refresh token es invalido");
        }
        if (!jwtService.esRefresh(claims)) {
            throw new TokenInvalidoException("TOKEN_WRONG_TYPE", "Enviaste un access token: aqui va el refreshToken");
        }
        Usuario u = usuarioRepository.findByEmailIgnoreCase(claims.getSubject())
                .orElseThrow(() -> new TokenInvalidoException("USER_NOT_FOUND", "La cuenta ya no existe"));
        if (!u.isEnabled()) {
            throw new TokenInvalidoException("ACCOUNT_DISABLED", "Tu cuenta esta desactivada");
        }
        if (!jwtService.coincideVersion(claims, u)) {
            throw new TokenInvalidoException("TOKEN_REVOKED", "Tu sesion fue cerrada. Inicia sesion de nuevo");
        }
        return tokenFactory.renovarAccess(u, request.refreshToken());
    }

    /** Logout real del lado del servidor: incrementa tokenVersion y revoca access y refresh en todos los dispositivos. */
    @Override
    @Transactional
    public MensajeResponse logout(Usuario actual) {
        Usuario u = recargar(actual);
        u.revocarTokens();
        usuarioRepository.save(u);
        return MensajeResponse.of("Sesion cerrada. Tus tokens quedaron revocados en todos los dispositivos.");
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Usuario actual) {
        return tokenFactory.perfil(recargar(actual));
    }

    /** Revoca todo lo anterior y devuelve tokens nuevos para seguir conectado en este dispositivo. */
    @Override
    @Transactional
    public AuthResponse cambiarPassword(Usuario actual, CambiarPasswordRequest r) {
        Usuario u = recargar(actual);
        if (!passwordEncoder.matches(r.passwordActual(), u.getPassword())) {
            throw new ReglaNegocioException("La contrasena actual no es correcta");
        }
        if (passwordEncoder.matches(r.nuevaPassword(), u.getPassword())) {
            throw new ReglaNegocioException("La nueva contrasena debe ser distinta de la actual");
        }
        u.setPassword(passwordEncoder.encode(r.nuevaPassword()));
        u.revocarTokens();
        usuarioRepository.save(u);
        return tokenFactory.emitir(u);
    }

    /**
     * Genera un token aleatorio de un solo uso (valido N minutos). En la BD solo se guarda su SHA-256:
     * aunque alguien lea la tabla, no puede usar el token.
     */
    @Override
    @Transactional
    public RecuperacionResponse recuperar(RecuperarRequest r) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(r.email()).filter(Usuario::isActivo);
        if (encontrado.isEmpty()) {
            return new RecuperacionResponse(MSG_RECUPERACION, null, minutosRecuperacion);
        }
        Usuario u = encontrado.get();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        u.setResetTokenHash(sha256(token));
        u.setResetTokenExpira(LocalDateTime.now(clock).plusMinutes(minutosRecuperacion));
        usuarioRepository.save(u);
        log.info("[RECUPERACION] Token para {} (simula el correo): {}", u.getEmail(), token);
        return new RecuperacionResponse(MSG_RECUPERACION, mostrarToken ? token : null, minutosRecuperacion);
    }

    @Override
    @Transactional
    public MensajeResponse restablecer(RestablecerRequest r) {
        Usuario u = usuarioRepository.findByResetTokenHash(sha256(r.token()))
                .orElseThrow(() -> new ReglaNegocioException("El enlace de recuperacion es invalido o ya fue usado"));
        if (u.getResetTokenExpira() == null || u.getResetTokenExpira().isBefore(LocalDateTime.now(clock))) {
            u.setResetTokenHash(null);
            u.setResetTokenExpira(null);
            usuarioRepository.save(u);
            throw new ReglaNegocioException("El enlace de recuperacion vencio. Solicita uno nuevo");
        }
        u.setPassword(passwordEncoder.encode(r.nuevaPassword()));
        u.setResetTokenHash(null);
        u.setResetTokenExpira(null);
        u.revocarTokens();
        usuarioRepository.save(u);
        return MensajeResponse.of("Contrasena restablecida. Ya puedes iniciar sesion.");
    }

    private Usuario recargar(Usuario actual) {
        return usuarioRepository.findById(actual.getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", actual.getId()));
    }

    static String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
