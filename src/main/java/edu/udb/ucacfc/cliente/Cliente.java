package edu.udb.ucacfc.cliente;

import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persona o empresa a cuyo nombre se hacen inscripciones, reservas, catering y cotizaciones.
 * usuario: cuenta de acceso OPCIONAL (cliente de mostrador = sin cuenta). Una cuenta CLIENTE siempre
 * tiene exactamente una ficha de cliente (se crean juntas en el registro o con POST /clientes/{id}/cuenta).
 * Nunca se borra (lo referencian muchas tablas): se desactiva.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoCliente tipo;

    @Column(name = "dui_nit", nullable = false, unique = true, length = 20)
    private String duiNit;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String empresa;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(unique = true, length = 9)
    private String telefono;

    @Column(length = 250)
    private String direccion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    /** Nombre para mostrar: "Empresa (contacto)" o el nombre de la persona. */
    public String nombreMostrado() {
        return tipo == TipoCliente.EMPRESA && empresa != null ? empresa + " (" + nombre + ")" : nombre;
    }
}
