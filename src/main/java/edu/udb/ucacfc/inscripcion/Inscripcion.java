package edu.udb.ucacfc.inscripcion;

import edu.udb.ucacfc.academico.oferta.Curso;
import edu.udb.ucacfc.academico.oferta.Diplomado;
import edu.udb.ucacfc.academico.oferta.OfertaAcademica;
import edu.udb.ucacfc.academico.oferta.TipoOferta;
import edu.udb.ucacfc.cliente.Cliente;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inscripcion de UN participante en UN curso o UN diplomado (nunca ambos, nunca ninguno: lo valida el service).
 * Si el cliente es EMPRESA, el participante es un colaborador (participanteNombre/Dui obligatorios) y la empresa
 * puede inscribir a varios; si es PERSONA, el participante es el propio cliente.
 * monto: precio de la oferta AL MOMENTO de inscribirse (si luego cambia el costo, esta inscripcion conserva el suyo).
 */
@Entity
@Table(name = "inscripciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inscripcion extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diplomado_id")
    private Diplomado diplomado;

    @Column(name = "participante_nombre", length = 150)
    private String participanteNombre;

    @Column(name = "participante_dui", length = 10)
    private String participanteDui;

    @Column(name = "participante_correo", length = 150)
    private String participanteCorreo;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoInscripcion estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @Version
    private Long version;

    public OfertaAcademica getOferta() {
        return curso != null ? curso : diplomado;
    }

    public TipoOferta getTipoOferta() {
        return curso != null ? TipoOferta.CURSO : TipoOferta.DIPLOMADO;
    }

    public String nombreParticipante() {
        return participanteNombre != null ? participanteNombre : cliente.getNombre();
    }
}
