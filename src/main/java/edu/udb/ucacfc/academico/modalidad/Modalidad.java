package edu.udb.ucacfc.academico.modalidad;

import edu.udb.ucacfc.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Presencial, virtual, hibrida... requiereEspacioFisico es la regla clave:
 * true  -> la oferta DEBE tener un espacio (y ocupa la agenda);
 * false -> la oferta NO puede tener espacio (un aula para un curso virtual se rechaza).
 */
@Entity
@Table(name = "modalidades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Modalidad extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "requiere_espacio_fisico", nullable = false)
    private boolean requiereEspacioFisico;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
