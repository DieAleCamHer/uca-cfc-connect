package edu.udb.ucacfc.cliente;

import edu.udb.ucacfc.seguridad.Usuario;
import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dui_nit", nullable = false, unique = true, length = 20)
    private String duiNit;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String empresa;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 250)
    private String direccion;

    // Un cliente puede tener una cuenta de usuario para iniciar sesion (rol CLIENTE).
    // Es opcional: la recepcionista puede registrar clientes que no usan el sistema directamente.
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
