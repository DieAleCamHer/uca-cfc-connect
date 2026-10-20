package edu.udb.ucacfc.seguridad.auth;

import edu.udb.ucacfc.seguridad.auth.dto.AuthResponse;
import edu.udb.ucacfc.seguridad.auth.dto.CambiarPasswordRequest;
import edu.udb.ucacfc.seguridad.auth.dto.LoginRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RecuperacionResponse;
import edu.udb.ucacfc.seguridad.auth.dto.RecuperarRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RefreshRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RegistroRequest;
import edu.udb.ucacfc.seguridad.auth.dto.RestablecerRequest;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioResponse;
import edu.udb.ucacfc.shared.security.Permisos;
import edu.udb.ucacfc.shared.web.MensajeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "01. Autenticacion", description = "Login, registro de clientes, refresh, logout y recuperacion de contrasena")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesion (publico)",
            description = "Devuelve accessToken (30 min) y refreshToken (7 dias). 401 BAD_CREDENTIALS si el correo o la "
                    + "contrasena no coinciden; 401 ACCOUNT_DISABLED si la cuenta fue desactivada.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Registrarse como cliente (publico)",
            description = "Crea la cuenta con rol CLIENTE (siempre: no se acepta campo rol) y su ficha de cliente en una "
                    + "sola transaccion, y deja la sesion iniciada. 409 si el correo, DUI/NIT o telefono ya existen. "
                    + "EMPRESA exige razon social y NIT.")
    public AuthResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar el accessToken (publico)",
            description = "Recibe el refreshToken en el body. 401 REFRESH_EXPIRED / REFRESH_INVALID / TOKEN_WRONG_TYPE / "
                    + "TOKEN_REVOKED / ACCOUNT_DISABLED.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @PreAuthorize(Permisos.AUTENTICADO)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cerrar sesion", description = "Revoca en el servidor todos los tokens de la cuenta (access y refresh).")
    public MensajeResponse logout(@Parameter(hidden = true) @AuthenticationPrincipal Usuario actual) {
        return authService.logout(actual);
    }

    @GetMapping("/me")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Mi perfil", description = "Datos de la cuenta autenticada; clienteId viene solo para rol CLIENTE.")
    public UsuarioResponse me(@Parameter(hidden = true) @AuthenticationPrincipal Usuario actual) {
        return authService.perfil(actual);
    }

    @PutMapping("/password")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Cambiar mi contrasena",
            description = "Exige la contrasena actual. Revoca las sesiones anteriores y devuelve tokens nuevos.")
    public AuthResponse cambiarPassword(@Parameter(hidden = true) @AuthenticationPrincipal Usuario actual,
                                        @Valid @RequestBody CambiarPasswordRequest request) {
        return authService.cambiarPassword(actual, request);
    }

    @PostMapping("/recuperar")
    @SecurityRequirements
    @Operation(summary = "Solicitar recuperacion de contrasena (publico)",
            description = "Responde siempre el mismo mensaje (no revela si el correo existe). Como no hay servidor de "
                    + "correo, en modo academico devuelve tokenDemo y lo escribe en el log. Vence en 30 minutos.")
    public RecuperacionResponse recuperar(@Valid @RequestBody RecuperarRequest request) {
        return authService.recuperar(request);
    }

    @PostMapping("/restablecer")
    @SecurityRequirements
    @Operation(summary = "Restablecer contrasena con el token (publico)",
            description = "El token es de un solo uso. Revoca todas las sesiones abiertas de la cuenta.")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerRequest request) {
        return authService.restablecer(request);
    }
}
