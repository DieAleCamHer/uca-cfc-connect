package edu.udb.ucacfc.agenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventoRepository extends JpaRepository<EventoInstitucional, Long>,
        JpaSpecificationExecutor<EventoInstitucional> {
}
