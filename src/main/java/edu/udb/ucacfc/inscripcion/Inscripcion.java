package edu.udb.ucacfc.inscripcion;

import edu.udb.ucacfc.academico.Curso;
import edu.udb.ucacfc.academico.Diplomado;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Una inscripcion pertenece SIEMPRE a un Cliente y a exactamente UNA
 * oferta academica: o un Curso, o un Diplomado (nunca ambos, nunca ninguno).
 * Esa regla ("exactamente uno de los dos") se valida en el Service,
 * porque JPA no puede expresar esa restriccion por si solo.
 */
@Entity
@Table(name = "inscripciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscripcion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne
    @JoinColumn(name = "diplomado_id")
    private Diplomado diplomado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado;
}
