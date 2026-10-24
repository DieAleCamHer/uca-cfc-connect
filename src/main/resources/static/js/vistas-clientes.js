/* Clientes (personal) y "Mis datos" (cliente). */

const CAMPOS_CLIENTE = [
  { name: 'tipo', label: 'Tipo de cliente', type: 'select', required: true, options: op(ENUMS.TipoCliente) },
  { name: 'duiNit', label: 'DUI / NIT', required: true, placeholder: '01234567-8', ayuda: 'DUI ########-# o NIT ####-######-###-#' },
  { name: 'nombre', label: 'Nombre (persona o contacto)', required: true, maxlength: 150 },
  { name: 'empresa', label: 'Razon social', maxlength: 150, visible: v => v.tipo === 'EMPRESA' },
  { name: 'correo', label: 'Correo', type: 'email', required: true },
  { name: 'telefono', label: 'Telefono', placeholder: '7123-4567' },
  { name: 'direccion', label: 'Direccion', full: true, maxlength: 250 }
];

function vistaClientes(cont) {
  encabezado(cont, 'Clientes', 'Personas y empresas. Busca por nombre, empresa, DUI/NIT o correo.',
    h('button', { class: 'btn btn-primary', onclick: () => formulario(null) }, '+ Registrar cliente'));
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/clientes', sortInicial: 'nombre,asc',
    filtros: [{ name: 'q', label: 'Buscar', placeholder: 'Nombre, DUI, correo...' },
      { name: 'tipo', label: 'Tipo', type: 'select', options: op(ENUMS.TipoCliente) },
      { name: 'conCuenta', label: 'Acceso web', type: 'select', options: [{ value: 'true', label: 'Con cuenta' }, { value: 'false', label: 'De mostrador' }] },
      { name: 'activo', label: 'Estado', type: 'select', options: opBool }],
    columnas: [
      { titulo: 'Cliente', sort: 'nombre', render: f => h('div', null, h('b', null, f.nombreMostrado), h('div', { class: 'small muted' }, fmt.enum(f.tipo))) },
      { titulo: 'DUI/NIT', campo: 'duiNit', sort: 'duiNit' },
      { titulo: 'Contacto', render: f => h('div', null, f.correo, h('div', { class: 'small muted' }, f.telefono || '')) },
      { titulo: 'Acceso web', render: f => f.usuarioEmail ? h('span', { class: 'small' }, f.usuarioEmail) : h('span', { class: 'muted small' }, 'Sin cuenta') },
      { titulo: 'Estado', sort: 'activo', render: f => badge(f.activo) }
    ],
    acciones: f => [
      boton('Historial', () => historialCliente(f.id)),
      boton('Editar', () => formulario(f)),
      !f.usuarioId && f.activo ? boton('Crear acceso', () => crearCuenta(f)) : null,
      f.activo ? boton('Inscribir', () => irA('inscripciones?cliente=' + f.id)) : null,
      esAdmin() ? (f.activo
        ? boton('Desactivar', async () => {
          if (await confirmar('¿Desactivar a ' + f.nombreMostrado + '? Tambien se bloqueara su acceso web.', { textoBoton: 'Desactivar', clase: 'btn-danger' }) !== null)
            accion(() => api('/clientes/' + f.id, { method: 'DELETE' }), 'Cliente desactivado', lista.recargar);
        }, 'btn-danger')
        : boton('Activar', () => accion(() => api('/clientes/' + f.id + '/activar', { method: 'PATCH' }), 'Cliente activado', lista.recargar))) : null
    ]
  });

  function formulario(fila) {
    modalFormulario({
      titulo: fila ? 'Editar cliente' : 'Registrar cliente', inicial: fila || { tipo: 'PERSONA' },
      campos: CAMPOS_CLIENTE.map(c => c.name === 'tipo' && fila ? { ...c, readonly: true, ayuda: 'El tipo no se puede cambiar' } : c),
      onGuardar: async v => {
        await api('/clientes' + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body: v });
        toast('Cliente guardado', 'ok');
        lista.recargar();
      }
    });
  }

  function crearCuenta(f) {
    modalFormulario({
      titulo: 'Crear acceso web para ' + f.nombreMostrado, textoGuardar: 'Crear cuenta',
      inicial: { email: f.correo },
      campos: [{ name: 'email', label: 'Correo de acceso', type: 'email', ayuda: 'Por defecto el de la ficha' },
        { name: 'password', label: 'Contrasena temporal', type: 'password', required: true }],
      onGuardar: async v => { await api('/clientes/' + f.id + '/cuenta', { method: 'POST', body: v }); toast('Cuenta creada', 'ok'); lista.recargar(); }
    });
  }
}

async function historialCliente(id) {
  try {
    const hst = await api('/clientes/' + id + '/historial');
    const conteo = (titulo, mapa) => h('div', { class: 'card kpi' }, h('div', { class: 'etiqueta' }, titulo),
      h('div', { class: 'small' }, Object.entries(mapa).filter(([, n]) => n > 0).map(([k, n]) => h('div', null, fmt.enum(k) + ': ' + n))),
      Object.values(mapa).every(n => !n) ? h('div', { class: 'muted small' }, 'Sin registros') : null);
    modal({
      titulo: 'Historial de ' + hst.cliente.nombreMostrado, grande: true,
      cuerpo: h('div', null,
        h('div', { class: 'grid grid-3' },
          h('div', { class: 'card kpi' }, h('div', { class: 'etiqueta' }, 'Facturado'), h('div', { class: 'valor' }, fmt.dinero(hst.totalFacturado))),
          h('div', { class: 'card kpi' }, h('div', { class: 'etiqueta' }, 'Pagado'), h('div', { class: 'valor' }, fmt.dinero(hst.totalPagado))),
          h('div', { class: 'card kpi' }, h('div', { class: 'etiqueta' }, 'Saldo pendiente'), h('div', { class: 'valor' }, fmt.dinero(hst.saldoPendiente)))),
        h('div', { class: 'grid grid-4', style: 'margin-top:1rem' }, conteo('Inscripciones', hst.inscripciones), conteo('Reservas', hst.reservas),
          conteo('Catering', hst.catering), conteo('Cotizaciones', hst.cotizaciones)),
        h('h3', { style: 'margin-top:1rem' }, 'Movimientos recientes'),
        hst.recientes.length ? h('table', null, h('thead', null, h('tr', null, ['Modulo', '#', 'Descripcion', 'Fecha', 'Estado', 'Monto'].map(t => h('th', null, t)))),
          h('tbody', null, hst.recientes.map(m => h('tr', null, h('td', null, fmt.enum(m.modulo)), h('td', null, m.id), h('td', null, m.descripcion),
            h('td', null, fmt.fechaHora(m.fecha)), h('td', null, badge(m.estado)), h('td', null, fmt.dinero(m.monto))))))
          : h('div', { class: 'vacio' }, 'Aun no hay movimientos'))
    });
  } catch (e) { toast(errorTexto(e), 'error'); }
}

async function vistaMisDatos(cont) {
  encabezado(cont, 'Mis datos', 'Tu ficha de cliente. El tipo y el DUI/NIT solo los corrige recepcion.');
  try {
    const c = await api('/clientes/me');
    const f = crearFormulario([
      { name: 'nombre', label: 'Nombre', required: true },
      { name: 'empresa', label: 'Razon social', visible: () => c.tipo === 'EMPRESA' },
      { name: 'correo', label: 'Correo', type: 'email', required: true },
      { name: 'telefono', label: 'Telefono', required: true, placeholder: '7123-4567' },
      { name: 'direccion', label: 'Direccion', full: true }
    ], c);
    cont.appendChild(h('div', { class: 'card' },
      detalle([['Tipo', fmt.enum(c.tipo)], ['DUI / NIT', c.duiNit], ['Cliente desde', fmt.fecha(c.fechaCreacion)]]),
      h('div', { style: 'margin-top:1rem' }, f.errorGeneral, f.form),
      h('div', { class: 'row', style: 'margin-top:.8rem' },
        h('button', {
          class: 'btn btn-primary', onclick: async () => {
            f.mostrarError(null);
            try { await api('/clientes/me', { method: 'PUT', body: f.valores() }); toast('Datos actualizados', 'ok'); } catch (e) { f.mostrarError(e); }
          }
        }, 'Guardar cambios'),
        h('button', { class: 'btn', onclick: () => historialCliente(c.id) }, 'Ver mi historial'))));
  } catch (e) { cont.appendChild(alertaError(e)); }
}

/** Opciones de clientes activos para formularios del personal (incluye el tipo para saber si es empresa). */
const CACHE_CLIENTES = {};
async function opcionesClientes() {
  const lista = await apiTodos('/clientes', { activo: true, sort: 'nombre,asc' });
  lista.forEach(c => { CACHE_CLIENTES[c.id] = c; });
  return lista.map(c => ({ value: c.id, label: `${c.nombreMostrado} - ${c.duiNit}` }));
}
