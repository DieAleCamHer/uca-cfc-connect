/* Pagos: cuentas por cobrar (las genera el sistema), abonos, validacion de transferencias y comprobantes. */

function metodosPermitidos() {
  return esCliente() ? ['TRANSFERENCIA', 'DEPOSITO'] : ENUMS.MetodoPago;
}

function vistaPagos(cont) {
  encabezado(cont, esCliente() ? 'Mis pagos' : 'Cuentas por cobrar',
    esCliente() ? 'Tus cuentas y abonos. Puedes reportar transferencias o depositos; el efectivo y la tarjeta se pagan en caja.'
      : 'Se generan solas al confirmar una inscripcion, reserva o catering, o al aprobar una cotizacion.');
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/pagos', sortInicial: 'id,desc',
    filtros: [{ name: 'q', label: 'Concepto', placeholder: 'Buscar...' },
      { name: 'estado', label: 'Estado', type: 'select', options: op(ENUMS.EstadoPago) },
      { name: 'tipoReferencia', label: 'Proceso', type: 'select', options: op(ENUMS.TipoReferencia) },
      { name: 'conSaldo', label: 'Saldo', type: 'select', options: [{ value: 'true', label: 'Con saldo pendiente' }] },
      { name: 'desde', label: 'Desde', type: 'date' }, { name: 'hasta', label: 'Hasta', type: 'date' }],
    columnas: [
      { titulo: '#', campo: 'id', sort: 'id' },
      { titulo: 'Concepto', render: f => h('div', null, f.concepto, esCliente() ? null : h('div', { class: 'small muted' }, f.clienteNombre)) },
      { titulo: 'Generada', sort: 'fechaGeneracion', render: f => fmt.fecha(f.fechaGeneracion) },
      { titulo: 'Total', sort: 'montoTotal', render: f => fmt.dinero(f.montoTotal) },
      { titulo: 'Pagado', sort: 'montoPagado', render: f => fmt.dinero(f.montoPagado) },
      { titulo: 'Saldo', render: f => h('div', null, fmt.dinero(f.saldo), f.enValidacion > 0 ? h('div', { class: 'small muted' }, fmt.dinero(f.enValidacion) + ' en validacion') : null) },
      { titulo: 'Estado', sort: 'estado', render: f => badge(f.estado) }
    ],
    acciones: f => [boton('Ver', () => detallePago(f.id, lista.recargar)),
      ['PENDIENTE', 'PARCIAL'].includes(f.estado) ? boton(esCliente() ? 'Reportar pago' : 'Registrar abono', () => formularioAbono(f, lista.recargar), 'btn-primary') : null]
  });
}

async function detallePago(id, alCambiar) {
  try {
    const p = await api('/pagos/' + id);
    const m = modal({
      titulo: 'Cuenta #' + p.id, grande: true,
      cuerpo: h('div', null,
        detalle([['Cliente', p.clienteNombre], ['Concepto', p.concepto], ['Proceso', fmt.enum(p.tipoReferencia) + ' #' + p.referenciaId],
          ['Estado', badge(p.estado)], ['Total', fmt.dinero(p.montoTotal)], ['Pagado (validado)', fmt.dinero(p.montoPagado)],
          ['Saldo', fmt.dinero(p.saldo)], ['En validacion', fmt.dinero(p.enValidacion)], ['Generada', fmt.fechaHora(p.fechaGeneracion)],
          ['Pagada por completo', fmt.fechaHora(p.fechaPagoCompleto)], p.motivoAnulacion ? ['Motivo de anulacion', p.motivoAnulacion] : null]),
        h('h3', { style: 'margin-top:1rem' }, 'Abonos'),
        p.abonos.length ? tablaAbonos(p.abonos, () => { m.cerrar(); detallePago(id, alCambiar); if (alCambiar) alCambiar(); })
          : h('div', { class: 'vacio' }, 'Sin abonos registrados')),
      acciones: ['PENDIENTE', 'PARCIAL'].includes(p.estado)
        ? [{ texto: esCliente() ? 'Reportar pago' : 'Registrar abono', clase: 'btn-primary', onClick: cerrar => { cerrar(); formularioAbono(p, alCambiar); } }] : []
    });
  } catch (e) { toast(errorTexto(e), 'error'); }
}

function accionesAbono(a, recargar) {
  return [
    a.estado === 'VALIDADO' ? boton('Comprobante', () => verComprobante(a.id)) : null,
    puede(R.FINANZAS) && a.estado === 'PENDIENTE_VALIDACION' ? boton('Validar', () => accion(() => api(`/pagos/abonos/${a.id}/validar`, { method: 'PATCH' }), 'Abono validado', recargar), 'btn-ok') : null,
    puede(R.FINANZAS) && a.estado === 'PENDIENTE_VALIDACION' ? boton('Rechazar', async () => {
      const m = await confirmar('Rechazar el abono #' + a.id + ' (el dinero no se encontro)', { motivo: true, motivoObligatorio: true, textoBoton: 'Rechazar', clase: 'btn-danger' });
      if (m !== null) accion(() => api(`/pagos/abonos/${a.id}/rechazar`, { method: 'PATCH', body: { motivo: m } }), 'Abono rechazado', recargar);
    }, 'btn-danger') : null,
    puede(R.FINANZAS) && a.estado === 'VALIDADO' ? boton('Anular', async () => {
      const m = await confirmar('Anular el abono #' + a.id + ' registrado por error. Se restara de la cuenta.', { motivo: true, motivoObligatorio: true, textoBoton: 'Anular', clase: 'btn-danger' });
      if (m !== null) accion(() => api(`/pagos/abonos/${a.id}/anular`, { method: 'PATCH', body: { motivo: m } }), 'Abono anulado', recargar);
    }, 'btn-danger') : null
  ];
}

function tablaAbonos(abonos, recargar) {
  return h('table', null, h('thead', null, h('tr', null, ['#', 'Fecha', 'Monto', 'Metodo', 'Referencia', 'Registro', 'Estado', ''].map(t => h('th', null, t)))),
    h('tbody', null, abonos.map(a => h('tr', null, h('td', null, a.id), h('td', null, fmt.fechaHora(a.fechaRegistro)), h('td', null, fmt.dinero(a.monto)),
      h('td', null, fmt.enum(a.metodo)), h('td', null, a.referenciaExterna || ''), h('td', null, a.registradoPor, h('div', { class: 'small muted' }, fmt.enum(a.rolRegistro))),
      h('td', null, badge(a.estado), a.motivo ? h('div', { class: 'small muted' }, a.motivo) : null, a.numeroComprobante ? h('div', { class: 'small muted' }, a.numeroComprobante) : null),
      h('td', { class: 'acciones' }, accionesAbono(a, recargar))))));
}

function formularioAbono(cuenta, alGuardar) {
  const ayuda = esCliente() ? 'Contabilidad validara tu transferencia o deposito antes de sumarlo.'
    : ROL === 'RECEPCIONISTA' ? 'Efectivo y tarjeta quedan confirmados al instante; transferencias y depositos pasan a validacion.'
      : 'Como Contabilidad/Administracion, el abono queda validado al registrarlo.';
  modalFormulario({
    titulo: (esCliente() ? 'Reportar pago - ' : 'Registrar abono - ') + 'cuenta #' + cuenta.id, textoGuardar: 'Registrar',
    inicial: { monto: Math.max(0, (cuenta.saldo || 0) - (cuenta.enValidacion || 0)).toFixed(2), metodo: metodosPermitidos()[0] },
    campos: [
      { name: 'monto', label: 'Monto ($)', type: 'number', required: true, min: 0.01, step: '0.01', ayuda: 'Saldo disponible: ' + fmt.dinero((cuenta.saldo || 0) - (cuenta.enValidacion || 0)) },
      { name: 'metodo', label: 'Metodo', type: 'select', required: true, options: op(metodosPermitidos()) },
      { name: 'referenciaExterna', label: 'No. de referencia', full: true, maxlength: 60, ayuda: 'Obligatorio en transferencia y deposito (voucher en tarjeta)',
        visible: v => v.metodo !== 'EFECTIVO' }
    ],
    extra: () => h('div', { class: 'alerta alerta-info', style: 'margin-top:1rem' }, ayuda),
    onGuardar: async v => {
      const a = await api(`/pagos/${cuenta.id}/abonos`, { method: 'POST', body: v });
      toast(a.estado === 'VALIDADO' ? 'Abono registrado. Comprobante ' + a.numeroComprobante : 'Pago reportado: pendiente de validacion', 'ok');
      if (alGuardar) alGuardar();
      if (a.estado === 'VALIDADO') verComprobante(a.id);
    }
  });
}

async function verComprobante(abonoId) {
  try {
    const c = await api(`/pagos/abonos/${abonoId}/comprobante`);
    modal({
      titulo: 'Comprobante ' + c.numero,
      cuerpo: h('div', { class: 'comprobante imprimible' },
        h('div', { class: 'encabezado' }, h('div', null, h('h2', null, c.emisor), h('div', { class: 'muted' }, 'Comprobante de pago')),
          h('div', { class: 'right' }, h('b', null, c.numero), h('div', { class: 'small' }, fmt.fechaHora(c.fechaEmision)))),
        detalle([['Cliente', c.clienteNombre], ['DUI / NIT', c.clienteDocumento], ['Correo', c.clienteCorreo],
          ['Concepto', c.concepto], ['Proceso', fmt.enum(c.tipoReferencia) + ' #' + c.referenciaId],
          ['Monto recibido', h('b', null, fmt.dinero(c.montoAbonado))], ['Metodo', fmt.enum(c.metodo) + (c.referenciaExterna ? ' - ' + c.referenciaExterna : '')],
          ['Total de la cuenta', fmt.dinero(c.totalCuenta)], ['Total pagado', fmt.dinero(c.totalPagado)], ['Saldo pendiente', fmt.dinero(c.saldoPendiente)],
          ['Estado de la cuenta', fmt.enum(c.estadoCuenta)], ['Registrado por', c.registradoPor], ['Validado por', c.validadoPor]])),
      acciones: [{ texto: 'Imprimir', clase: 'btn-primary', onClick: () => window.print() }]
    });
  } catch (e) { toast(errorTexto(e), 'error'); }
}

function vistaAbonos(cont) {
  const finanzas = puede(R.FINANZAS);
  encabezado(cont, finanzas ? 'Validacion de pagos' : 'Abonos',
    finanzas ? 'Bandeja de transferencias y depositos por validar, y todos los abonos registrados.' : 'Abonos registrados y sus comprobantes.');
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/pagos/abonos', sortInicial: 'id,desc',
    filtrosIniciales: finanzas ? { estado: 'PENDIENTE_VALIDACION' } : {},
    filtros: [{ name: 'estado', label: 'Estado', type: 'select', options: op(ENUMS.EstadoAbono) },
      { name: 'metodo', label: 'Metodo', type: 'select', options: op(ENUMS.MetodoPago) },
      { name: 'desde', label: 'Desde', type: 'date' }, { name: 'hasta', label: 'Hasta', type: 'date' }],
    columnas: [
      { titulo: '#', campo: 'id', sort: 'id' },
      { titulo: 'Cuenta', render: f => h('div', null, h('a', { href: '#', onclick: e => { e.preventDefault(); detallePago(f.pagoId); } }, '#' + f.pagoId + ' ' + f.concepto),
        esCliente() ? null : h('div', { class: 'small muted' }, f.clienteNombre)) },
      { titulo: 'Fecha', sort: 'fechaRegistro', render: f => fmt.fechaHora(f.fechaRegistro) },
      { titulo: 'Monto', sort: 'monto', render: f => fmt.dinero(f.monto) },
      { titulo: 'Metodo', sort: 'metodo', render: f => h('div', null, fmt.enum(f.metodo), f.referenciaExterna ? h('div', { class: 'small muted' }, f.referenciaExterna) : null) },
      { titulo: 'Registro', render: f => h('div', null, f.registradoPor, h('div', { class: 'small muted' }, fmt.enum(f.rolRegistro))) },
      { titulo: 'Estado', sort: 'estado', render: f => h('div', null, badge(f.estado), f.motivo ? h('div', { class: 'small muted' }, f.motivo) : null) }
    ],
    acciones: (f, l) => accionesAbono(f, () => l.recargar())
  });
}
