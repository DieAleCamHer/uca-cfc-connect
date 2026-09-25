package edu.udb.ucacfc.academico;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

// Nota: no usamos @Builder aqui porque Lombok complica el builder cuando
// hay herencia con @MappedSuperclass. Se usa el constructor vacio + setters,
// que es exactamente el mismo patron que ya usaron en las Guias 1-4.
@Entity
@Table(name = "cursos")
@NoArgsConstructor
public class Curso extends OfertaAcademica {
}
