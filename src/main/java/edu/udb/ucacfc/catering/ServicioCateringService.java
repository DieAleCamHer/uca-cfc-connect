package edu.udb.ucacfc.catering;

import java.util.List;

public interface ServicioCateringService {
    ServicioCatering crear(ServicioCatering servicio);
    ServicioCatering buscarPorId(Long id);
    List<ServicioCatering> listar();
    ServicioCatering actualizar(Long id, ServicioCatering datos);
    void eliminar(Long id);
}
