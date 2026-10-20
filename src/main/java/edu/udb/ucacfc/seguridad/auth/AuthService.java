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
import edu.udb.ucacfc.shared.web.MensajeResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse registrar(RegistroRequest request);

    AuthResponse refresh(RefreshRequest request);

    MensajeResponse logout(Usuario actual);

    UsuarioResponse perfil(Usuario actual);

    AuthResponse cambiarPassword(Usuario actual, CambiarPasswordRequest request);

    RecuperacionResponse recuperar(RecuperarRequest request);

    MensajeResponse restablecer(RestablecerRequest request);
}
