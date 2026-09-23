package edu.udb.ucacfc.academico;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diplomados")
@NoArgsConstructor
public class Diplomado extends OfertaAcademica {
}
