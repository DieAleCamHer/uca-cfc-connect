package edu.udb.ucacfc.cotizacion;

import java.util.List;

public interface CotizacionService {
    Cotizacion crear(Cotizacion cotizacion);
    Cotizacion buscarPorId(Long id);
    List<Cotizacion> listar();
    List<Cotizacion> listarPorCliente(Long clienteId);
    Cotizacion cambiarEstado(Long id, EstadoCotizacion nuevoEstado);
}
