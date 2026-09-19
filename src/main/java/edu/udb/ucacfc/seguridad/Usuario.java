package edu.udb.ucacfc.seguridad;

import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuario implementa UserDetails directamente: es la tecnica estandar de
 * Spring Security + JWT (la que enseña Baeldung, uno de tus recursos).
 * Asi UsuarioDetailsServiceImpl puede devolver el Usuario tal cual, sin
 * necesidad de una clase adaptadora aparte.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends Auditable implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Aqui se guarda el hash (BCrypt), nunca la contrasena en texto plano.
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    // ---- Metodos exigidos por la interfaz UserDetails de Spring Security ----

    @Override
    public String getUsername() {
        // El "username" de login es el email (no hay un campo aparte).
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security espera el prefijo "ROLE_" para usar hasRole("X") en la config.
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Un usuario desactivado (activo=false) no puede autenticarse aunque
        // tenga la contrasena correcta.
        return activo;
    }
}
