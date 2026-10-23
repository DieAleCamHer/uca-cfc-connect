package edu.udb.ucacfc.academico.oferta;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Oferta de tipo CURSO. Toda la logica es compartida (OfertaAcademicaService). */
@Entity
@Table(name = "cursos")
@Getter
@Setter
@NoArgsConstructor
public class Curso extends OfertaAcademica {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "curso_sesiones", joinColumns = @JoinColumn(name = "curso_id"))
    @OrderBy("inicio ASC")
    private List<SesionOferta> sesiones = new ArrayList<>();

    @Override
    public TipoOferta getTipo() {
        return TipoOferta.CURSO;
    }
}
