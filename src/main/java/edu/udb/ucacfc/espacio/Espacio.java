package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Espacio fisico del CFC. activo = el espacio se puede usar (borrado logico).
 * "Ocupado en tal horario" NO es un campo: se calcula consultando la agenda institucional.
 * precioHora: base del costo del alquiler (horas x precioHora, lo calcula ReservaService).
 */
@Entity
@Table(name = "espacios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Espacio extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(name = "precio_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHora;

    @Column(length = 300)
    private String equipamiento;

    @Column(length = 150)
    private String ubicacion;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
