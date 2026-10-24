package edu.udb.ucacfc.cliente;

import edu.udb.ucacfc.shared.exception.RegistroDuplicadoException;
import edu.udb.ucacfc.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reglas de la ficha de cliente compartidas por el registro publico (AuthService) y el modulo Clientes.
 */
@Component
@RequiredArgsConstructor
public class ClienteValidador {

    private final ClienteRepository repo;

    /** idExcluir = null al crear; el id propio al editar (para no chocar consigo mismo). */
    public void validarUnicidad(String duiNit, String correo, String telefono, Long idExcluir) {
        boolean dui = idExcluir == null ? repo.existsByDuiNit(duiNit) : repo.existsByDuiNitAndIdNot(duiNit, idExcluir);
        if (dui) {
            throw new RegistroDuplicadoException("Ya existe un cliente con DUI/NIT " + duiNit
                    + (idExcluir == null ? ". Si ya eras cliente del CFC, acercate a recepcion para activar tu acceso." : ""));
        }
        boolean mail = idExcluir == null ? repo.existsByCorreoIgnoreCase(correo)
                : repo.existsByCorreoIgnoreCaseAndIdNot(correo, idExcluir);
        if (mail) {
            throw new RegistroDuplicadoException("Ya existe un cliente con el correo " + correo);
        }
        if (telefono != null && !telefono.isBlank()) {
            boolean tel = idExcluir == null ? repo.existsByTelefono(telefono)
                    : repo.existsByTelefonoAndIdNot(telefono, idExcluir);
            if (tel) {
                throw new RegistroDuplicadoException("Ya existe un cliente con el telefono " + telefono);
            }
        }
    }

    /** EMPRESA: razon social obligatoria y documento NIT (14 digitos). PERSONA: DUI o NIT. */
    public void validarTipo(TipoCliente tipo, String duiNit, String empresa) {
        if (tipo == TipoCliente.EMPRESA) {
            if (empresa == null || empresa.isBlank()) {
                throw new ReglaNegocioException("Un cliente de tipo EMPRESA debe indicar la razon social (empresa)");
            }
            if (duiNit.replace("-", "").length() != 14) {
                throw new ReglaNegocioException("Una EMPRESA se identifica con NIT (14 digitos), no con DUI");
            }
        }
    }
}
