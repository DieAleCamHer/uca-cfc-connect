package edu.udb.ucacfc.seguridad;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unico Controller publico (ver SecurityConfig: /api/auth/** esta en permitAll).
 * Aqui es donde entran los usuarios sin token todavia.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                           UsuarioService usuarioService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        // Si el email/password no coinciden, esto lanza BadCredentialsException,
        // que el GlobalExceptionHandler traduce a HTTP 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(); // no deberia pasar: si authenticate() no fallo, el usuario existe

        String token = jwtService.generarToken(usuario);
        return LoginResponseDTO.desde(token, usuario);
    }

    // Auto-registro publico: SIEMPRE crea el usuario con rol CLIENTE, sin
    // importar lo que venga en el DTO. Crear cuentas ADMIN/RECEPCIONISTA/
    // CONTABILIDAD solo lo puede hacer un ADMIN ya autenticado, via
    // POST /api/usuarios (ver SecurityConfig).
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registro(@Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario usuario = Usuario.builder()
                .nombre(dto.nombre())
                .email(dto.email())
                .password(dto.password())
                .rol(Rol.CLIENTE)
                .build();
        Usuario creado = usuarioService.registrar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.desde(creado));
    }
}
