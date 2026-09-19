package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.shared.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "espacios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Espacio extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(length = 300)
    private String equipamiento;

    @Builder.Default
    @Column(nullable = false)
    private boolean disponible = true;
}
