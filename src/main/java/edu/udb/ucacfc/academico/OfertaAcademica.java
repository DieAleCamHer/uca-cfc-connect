package edu.udb.ucacfc.academico;

import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Campos comunes entre Curso y Diplomado. No es una entidad propia
 * (no genera tabla): cada subclase concreta tiene su propia tabla,
 * con estos campos repetidos. Es la forma mas simple de evitar
 * duplicar codigo sin meterse en herencia JPA compleja (joined/single table).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class OfertaAcademica extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "modalidad_id")
    private Modalidad modalidad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "docente_id")
    private Docente docente;

    // Opcional: solo aplica si la modalidad requiere espacio fisico
    // (ver validacion en CursoServiceImpl/DiplomadoServiceImpl - una
    // modalidad VIRTUAL no puede tener espacio asignado).
    @ManyToOne
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @Column(name = "cupo_maximo", nullable = false)
    private Integer cupoMaximo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(nullable = false)
    private boolean activo = true;
}
