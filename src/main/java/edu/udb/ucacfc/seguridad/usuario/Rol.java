package edu.udb.ucacfc.seguridad.usuario;

import lombok.Getter;

import java.util.List;

/**
 * Los 4 roles del lineamiento con su descripcion y sus permisos funcionales.
 * La lista de permisos es la que se expone en GET /api/roles (modulo Seguridad: "Roles / Permisos")
 * y coincide con los @PreAuthorize de los controllers (shared/security/Permisos).
 */
@Getter
public enum Rol {
    ADMIN("Administrador", "Configuracion completa del sistema", List.of(
            "Administrar usuarios y roles", "Crear y desactivar cursos y diplomados",
            "Administrar catalogos (categorias, modalidades, docentes, espacios, servicios de catering)",
            "Aprobar cotizaciones", "Ver todos los pagos", "Consultar reportes", "Todo lo de Recepcionista")),
    RECEPCIONISTA("Recepcionista", "Principal usuario operativo", List.of(
            "Registrar clientes y participantes", "Crear inscripciones", "Registrar solicitudes de alquiler",
            "Registrar solicitudes de catering", "Registrar y elaborar cotizaciones",
            "Programar actividades (cursos, diplomados, eventos)", "Confirmar pagos en caja",
            "Consultar disponibilidad de espacios y la agenda institucional")),
    CLIENTE("Cliente", "Persona o empresa que contrata servicios", List.of(
            "Registrarse", "Consultar la oferta academica", "Inscribirse", "Solicitar cotizaciones",
            "Solicitar alquileres", "Solicitar catering", "Consultar el estado de sus solicitudes",
            "Consultar sus pagos y reportar transferencias/depositos")),
    CONTABILIDAD("Contabilidad", "Control financiero", List.of(
            "Registrar pagos", "Validar transferencias y depositos", "Emitir comprobantes",
            "Consultar estados financieros"));

    private final String nombre;
    private final String descripcion;
    private final List<String> permisos;

    Rol(String nombre, String descripcion, List<String> permisos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.permisos = permisos;
    }

    /** Roles internos (personal del CFC). CLIENTE no es personal: su cuenta nace con su ficha de cliente. */
    public boolean esPersonal() {
        return this != CLIENTE;
    }
}
