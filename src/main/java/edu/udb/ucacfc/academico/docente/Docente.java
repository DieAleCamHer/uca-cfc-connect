package edu.udb.ucacfc.academico.docente;

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

@Entity
@Table(name = "docentes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Docente extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String especialidad;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(unique = true, length = 9)
    private String telefono;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
