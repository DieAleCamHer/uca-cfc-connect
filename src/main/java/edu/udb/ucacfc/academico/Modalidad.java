package edu.udb.ucacfc.academico;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modalidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ej: PRESENCIAL, VIRTUAL, SEMIPRESENCIAL
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    // Clave para la regla de negocio: un espacio fisico no se puede reservar
    // para una oferta cuya modalidad es virtual.
    @Column(name = "requiere_espacio_fisico", nullable = false)
    private boolean requiereEspacioFisico;
}
