package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Catalogo de catering: precio POR PERSONA. */
@Entity
@Table(name = "servicios_catering", uniqueConstraints = @UniqueConstraint(columnNames = {"tipo", "nombre"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioCatering extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoServicioCatering tipo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "menu_base", length = 500)
    private String menuBase;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
