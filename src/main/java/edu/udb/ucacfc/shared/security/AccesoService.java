package edu.udb.ucacfc.shared.security;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.exception.AccesoDenegadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Tercera capa de seguridad: DUENO DEL REGISTRO.
 * @PreAuthorize ya decidio que el ROL puede usar el endpoint; aqui se decide si ESE registro le pertenece.
 * El personal (ADMIN, RECEPCIONISTA, CONTABILIDAD) no tiene restriccion de dueno.
 */
@Service
@RequiredArgsConstructor
public class AccesoService {

    private final ClienteRepository clienteRepository;

    public boolean esCliente(Usuario u) {
        return u != null && u.getRol() == Rol.CLIENTE;
    }

    /** Ficha de cliente de una cuenta CLIENTE. Toda cuenta CLIENTE nace con su ficha. */
    public Cliente clienteDe(Usuario u) {
        return clienteRepository.findByUsuarioId(u.getId())
                .orElseThrow(() -> new AccesoDenegadoException(
                        "Tu cuenta no tiene ficha de cliente asociada. Comunicate con recepcion."));
    }

    public Long clienteIdDe(Usuario u) {
        return clienteDe(u).getId();
    }

    /**
     * Cliente a cuyo nombre se crea un proceso: un CLIENTE siempre a su propio nombre (el clienteId del body
     * se ignora, asi nadie crea cosas a nombre de otro); el personal debe indicarlo.
     */
    public Long resolverClienteParaCrear(Usuario u, Long clienteIdBody) {
        if (esCliente(u)) {
            return clienteIdDe(u);
        }
        if (clienteIdBody == null) {
            throw new ReglaNegocioException("clienteId es obligatorio cuando el personal registra la solicitud");
        }
        return clienteIdBody;
    }

    /** Filtro de listados: un CLIENTE solo ve lo suyo aunque mande otro clienteId. */
    public Long filtroCliente(Usuario u, Long clienteIdFiltro) {
        return esCliente(u) ? clienteIdDe(u) : clienteIdFiltro;
    }

    /** Lanza 403 si un CLIENTE intenta ver o modificar un registro de otro cliente. */
    public void verificarDueno(Usuario u, Long clienteIdDelRegistro, String recurso) {
        if (esCliente(u) && !clienteIdDe(u).equals(clienteIdDelRegistro)) {
            throw new AccesoDenegadoException("No puedes acceder a " + recurso + " de otro cliente");
        }
    }
}
