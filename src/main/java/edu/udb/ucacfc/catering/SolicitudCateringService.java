package edu.udb.ucacfc.catering;

import java.util.List;

public interface SolicitudCateringService {
    SolicitudCatering solicitar(SolicitudCatering solicitud);
    SolicitudCatering buscarPorId(Long id);
    List<SolicitudCatering> listar();
    List<SolicitudCatering> listarPorCliente(Long clienteId);
    SolicitudCatering cambiarEstado(Long id, EstadoSolicitudCatering nuevoEstado);
}
