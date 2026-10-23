package edu.udb.ucacfc.academico.oferta;

import edu.udb.ucacfc.academico.categoria.Categoria;
import edu.udb.ucacfc.academico.docente.Docente;
import edu.udb.ucacfc.academico.modalidad.Modalidad;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Campos comunes de Curso y Diplomado (@MappedSuperclass: no tiene tabla propia; sus columnas se copian
 * a "cursos" y "diplomados"). inicio/fin son el RESUMEN de las sesiones (primer inicio y ultimo fin) y los
 * recalcula el service; sirven para filtrar, ordenar y saber si la oferta ya empezo o termino.
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

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modalidad_id", nullable = false)
    private Modalidad modalidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    /** Solo si la modalidad requiere espacio fisico. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @Column(name = "cupo_maximo", nullable = false)
    private Integer cupoMaximo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(nullable = false)
    private boolean activo = true;

    /** Bloqueo optimista: dos ediciones simultaneas de la misma oferta -> 409 en la segunda. */
    @Version
    private Long version;

    public abstract List<SesionOferta> getSesiones();

    public abstract TipoOferta getTipo();

    public boolean yaInicio(LocalDateTime ahora) {
        return !ahora.isBefore(inicio);
    }

    public boolean yaTermino(LocalDateTime ahora) {
        return !ahora.isBefore(fin);
    }
}
