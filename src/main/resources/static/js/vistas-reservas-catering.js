/* Reservas de espacio y solicitudes de catering. */
/* ------------------------------------------------------------------ Reservas de espacio */
function vistaReservas(cont) {
  const p = paramsHash();
  encabezado(cont, esCliente() ? 'Mis reservas' : 'Reservas de espacios',
    'SOLICITADA (ya bloquea el espacio) -> CONFIRMADA (genera la cuenta) -> COMPLETADA. Costo = horas x precio por hora.',
    h('button', { class: 'btn btn-primary', onclick: () => formulario(null, p) }, '+ Nueva reserva'));
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/reservas', sortInicial: 'inicio,desc',
    filtros: [{ name: 'estado', label: 'Estado', type: 'select', options: op(ENUMS.EstadoReserva) },
      { name: 'espacioId', label: 'Espacio', type: 'select', options: opcionesActivas('/espacios') },
      { name: 'desde', label: 'Desde', type: 'date' }, { name: 'hasta', label: 'Hasta', type: 'date' }],
    columnas: [
      { titulo: '#', campo: 'id', sort: 'id' },
      { titulo: 'Espacio', sort: 'espacio', render: f => h('div', null, h('b', null, f.espacioNombre), esCliente() ? null : h('div', { class: 'small muted' }, f.clienteNombre)) },
      { titulo: 'Horario', sort: 'inicio', render: f => fmt.rango(f.inicio, f.fin) },
      { titulo: 'Proposito', render: f => h('div', null, f.proposito, h('div', { class: 'small muted' }, f.numeroAsistentes + ' asistentes')) },
      { titulo: 'Total', sort: 'total', render: f => h('div', null, fmt.dinero(f.total), h('div', { class: 'small muted' }, f.horas + ' h x ' + fmt.dinero(f.precioHora))) },
      { titulo: 'Pago', render: columnaPago },
      { titulo: 'Estado', sort: 'estado', render: f => h('div', null, badge(f.estado), f.motivoEstado ? h('div', { class: 'small muted' }, f.motivoEstado) : null) }
    ],
    acciones: f => [
      f.estado === 'SOLICITADA' && !puede(['CONTABILIDAD']) ? boton('Editar', () => formulario(f)) : null,
      f.accionesPosibles.includes('CONFIRMADA') ? boton('Confirmar', () => accion(() => api(`/reservas/${f.id}/confirmar`, { method: 'PATCH' }), 'Reserva confirmada: se genero su cuenta', lista.recargar), 'btn-ok') : null,
      f.accionesPosibles.includes('RECHAZADA') ? boton('Rechazar', async () => {
        const m = await confirmar('Rechazar la reserva #' + f.id, { motivo: true, motivoObligatorio: true, textoBoton: 'Rechazar', clase: 'btn-danger' });
        if (m !== null) accion(() => api(`/reservas/${f.id}/rechazar`, { method: 'PATCH', body: { motivo: m } }), 'Reserva rechazada', lista.recargar);
      }, 'btn-danger') : null,
      f.accionesPosibles.includes('COMPLETADA') ? boton('Completar', () => accion(() => api(`/reservas/${f.id}/completar`, { method: 'PATCH' }), 'Completada', lista.recargar)) : null,
      f.accionesPosibles.includes('CANCELADA') ? boton('Cancelar', async () => {
        const m = await confirmar('¿Cancelar la reserva #' + f.id + '? Se liberara el espacio.', { motivo: true, textoBoton: 'Cancelar reserva', clase: 'btn-danger' });
        if (m !== null) accion(() => api('/reservas/' + f.id, { method: 'DELETE', query: { motivo: m } }), 'Reserva cancelada', lista.recargar);
      }, 'btn-danger') : null,
      ['SOLICITADA', 'CONFIRMADA'].includes(f.estado) && !puede(['CONTABILIDAD']) ? boton('Catering', () => irA('catering?reserva=' + f.id + '&cliente=' + f.clienteId)) : null,
      botonCuenta(f)
    ]
  });

  function formulario(fila, pre = {}) {
    const resultado = h('div', { style: 'margin-top:.6rem' });
    const inicial = fila ? { ...fila } : { clienteId: pre.cliente, espacioId: pre.espacio, inicio: pre.inicio, fin: pre.fin };
    const { f } = modalFormulario({
      titulo: fila ? 'Editar reserva #' + fila.id : 'Nueva reserva', inicial,
      campos: [
        esCliente() ? null : { name: 'clienteId', label: 'Cliente', type: 'select', required: true, numerico: true, options: opcionesClientes, full: true, readonly: !!fila },
        { name: 'espacioId', label: 'Espacio', type: 'select', required: true, numerico: true, full: true,
          options: opcionesActivas('/espacios', e => `${e.nombre} · ${fmt.enum(e.tipo)} · cap. ${e.capacidad} · ${fmt.dinero(e.precioHora)}/h`) },
        { name: 'inicio', label: 'Inicio', type: 'datetime-local', required: true },
        { name: 'fin', label: 'Fin', type: 'datetime-local', required: true, ayuda: 'Entre 30 minutos y 12 horas' },
        { name: 'numeroAsistentes', label: 'Asistentes', type: 'number', required: true, min: 1 },
        { name: 'proposito', label: 'Proposito del evento', required: true, maxlength: 200 }
      ].filter(Boolean),
      extra: fx => h('div', null, h('button', {
        class: 'btn btn-sm', type: 'button', style: 'margin-top:.8rem', onclick: async () => {
          const v = fx.valores();
          resultado.innerHTML = '';
          if (!v.inicio || !v.fin) { resultado.appendChild(h('div', { class: 'alerta alerta-info' }, 'Indica inicio y fin')); return; }
          try {
            const disp = await api('/espacios/disponibilidad', { query: { inicio: v.inicio, fin: v.fin, excluirTipo: fila ? 'ALQUILER' : null, excluirReferenciaId: fila ? fila.id : null } });
            const sel = disp.find(d => d.espacioId === v.espacioId);
            resultado.appendChild(sel
              ? h('div', { class: 'alerta ' + (sel.libre ? 'alerta-ok' : 'alerta-error') },
                sel.libre ? `Disponible. Costo estimado: ${fmt.dinero(sel.costoEstimado)}` : 'Ocupado por ' + sel.conflicto)
              : h('div', { class: 'alerta alerta-info' }, 'Libres en ese horario: ' + (disp.filter(d => d.libre).map(d => d.nombre).join(', ') || 'ninguno')));
          } catch (e) { resultado.appendChild(alertaError(e)); }
        }
      }, 'Verificar disponibilidad y costo'), resultado),
      onGuardar: async v => {
        await api('/reservas' + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body: { ...v, clienteId: v.clienteId ?? null } });
        toast(fila ? 'Reserva actualizada' : 'Reserva solicitada: el espacio quedo apartado', 'ok');
        lista.recargar();
      }
    });
    return f;
  }
  if (p.espacio) formulario(null, p);
}

/* ------------------------------------------------------------------ Catering */
function vistaCatering(cont) {
  const p = paramsHash();
  encabezado(cont, esCliente() ? 'Mis pedidos de catering' : 'Solicitudes de catering',
    'Se piden con 24 h de anticipacion. Total = precio por persona x asistentes (lo calcula el sistema).',
    h('button', { class: 'btn btn-primary', onclick: () => formulario(null, p) }, '+ Nueva solicitud'));
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/solicitudes-catering', sortInicial: 'inicio,desc',
    filtros: [{ name: 'estado', label: 'Estado', type: 'select', options: op(ENUMS.EstadoSolicitudCatering) },
      { name: 'servicioId', label: 'Servicio', type: 'select', options: opcionesActivas('/servicios-catering') },
      { name: 'desde', label: 'Desde', type: 'date' }],
    columnas: [
      { titulo: '#', campo: 'id', sort: 'id' },
      { titulo: 'Servicio', sort: 'servicio', render: f => h('div', null, h('b', null, f.servicioNombre), h('div', { class: 'small muted' }, esCliente() ? fmt.enum(f.tipoServicio) : f.clienteNombre)) },
      { titulo: 'Horario', sort: 'inicio', render: f => fmt.rango(f.inicio, f.fin) },
      { titulo: 'Lugar', render: f => h('div', null, f.lugar, f.reservaId ? h('div', { class: 'small muted' }, 'Dentro de la reserva #' + f.reservaId) : null) },
      { titulo: 'Asistentes', sort: 'numeroAsistentes', campo: 'numeroAsistentes' },
      { titulo: 'Total', sort: 'total', render: f => h('div', null, fmt.dinero(f.total), h('div', { class: 'small muted' }, fmt.dinero(f.precioUnitario) + ' c/u')) },
      { titulo: 'Pago', render: columnaPago },
      { titulo: 'Estado', sort: 'estado', render: f => h('div', null, badge(f.estado), f.motivoEstado ? h('div', { class: 'small muted' }, f.motivoEstado) : null) }
    ],
    acciones: f => [
      f.estado === 'PENDIENTE' && !puede(['CONTABILIDAD']) ? boton('Editar', () => formulario(f)) : null,
      f.accionesPosibles.includes('CONFIRMADA') ? boton('Confirmar', () => accion(() => api(`/solicitudes-catering/${f.id}/confirmar`, { method: 'PATCH' }), 'Confirmada: se genero su cuenta', lista.recargar), 'btn-ok') : null,
      f.accionesPosibles.includes('RECHAZADA') ? boton('Rechazar', async () => {
        const m = await confirmar('Rechazar la solicitud #' + f.id, { motivo: true, motivoObligatorio: true, textoBoton: 'Rechazar', clase: 'btn-danger' });
        if (m !== null) accion(() => api(`/solicitudes-catering/${f.id}/rechazar`, { method: 'PATCH', body: { motivo: m } }), 'Rechazada', lista.recargar);
      }, 'btn-danger') : null,
      f.accionesPosibles.includes('ATENDIDA') ? boton('Atendida', () => accion(() => api(`/solicitudes-catering/${f.id}/atender`, { method: 'PATCH' }), 'Marcada como atendida', lista.recargar)) : null,
      f.accionesPosibles.includes('CANCELADA') ? boton('Cancelar', async () => {
        const m = await confirmar('¿Cancelar la solicitud #' + f.id + '?', { motivo: true, textoBoton: 'Cancelar solicitud', clase: 'btn-danger' });
        if (m !== null) accion(() => api('/solicitudes-catering/' + f.id, { method: 'DELETE', query: { motivo: m } }), 'Solicitud cancelada', lista.recargar);
      }, 'btn-danger') : null,
      botonCuenta(f)
    ]
  });

  async function formulario(fila, pre = {}) {
    const ficha = await miFicha();
    const precios = {};
    const servicios = async () => (await apiTodos('/servicios-catering', { activo: true })).map(s => {
      precios[s.id] = s.precioUnitario;
      return { value: s.id, label: `${fmt.enum(s.tipo)} · ${s.nombre} · ${fmt.dinero(s.precioUnitario)} p/p` };
    });
    const reservasDe = async clienteId => {
      if (!clienteId) return [];
      const q = esCliente() ? {} : { clienteId };
      const todas = [...await apiTodos('/reservas', { ...q, estado: 'SOLICITADA' }), ...await apiTodos('/reservas', { ...q, estado: 'CONFIRMADA' })];
      return todas.map(r => ({ value: r.id, label: `#${r.id} · ${r.espacioNombre} · ${fmt.rango(r.inicio, r.fin)}` }));
    };
    const ubic = fila ? (fila.reservaId ? 'RESERVA' : fila.espacioId ? 'ESPACIO' : 'EXTERNO') : (pre.reserva ? 'RESERVA' : 'EXTERNO');
    const inicial = fila ? { ...fila, fecha: fila.inicio.slice(0, 10), horaInicio: fila.inicio.slice(11, 16), horaFin: fila.fin.slice(11, 16), ubicacion: ubic }
      : { clienteId: pre.cliente ? Number(pre.cliente) : (ficha ? ficha.id : null), servicioId: pre.servicio, reservaId: pre.reserva, ubicacion: ubic };
    const total = h('div', { class: 'alerta alerta-info', style: 'margin-top:.8rem' }, 'Total estimado: -');
    const calcular = fx => {
      const v = fx.valores();
      total.textContent = 'Total estimado: ' + (precios[v.servicioId] && v.numeroAsistentes ? fmt.dinero(precios[v.servicioId] * v.numeroAsistentes) : '-');
    };
    let formRef;
    const { f } = modalFormulario({
      titulo: fila ? 'Editar solicitud #' + fila.id : 'Nueva solicitud de catering', inicial,
      campos: [
        esCliente() ? null : {
          name: 'clienteId', label: 'Cliente', type: 'select', required: true, numerico: true, options: opcionesClientes, full: true, readonly: !!fila,
          onchange: async (val, fx) => fx.setOpciones('reservaId', await reservasDe(val), fx.valores().reservaId)
        },
        { name: 'servicioId', label: 'Servicio', type: 'select', required: true, numerico: true, options: servicios, full: true, onchange: (v, fx) => calcular(fx) },
        { name: 'numeroAsistentes', label: 'Asistentes', type: 'number', required: true, min: 1, max: 1000, onchange: (v, fx) => calcular(fx) },
        { name: 'fecha', label: 'Fecha', type: 'date', required: true },
        { name: 'horaInicio', label: 'Hora inicio', type: 'time', required: true },
        { name: 'horaFin', label: 'Hora fin', type: 'time', required: true },
        { name: 'ubicacion', label: '¿Donde se sirve?', type: 'select', required: true,
          options: [{ value: 'EXTERNO', label: 'Lugar externo' }, { value: 'ESPACIO', label: 'Espacio del CFC (lo aparta)' }, { value: 'RESERVA', label: 'Dentro de una reserva mia' }] },
        { name: 'espacioId', label: 'Espacio', type: 'select', numerico: true, options: opcionesActivas('/espacios', e => `${e.nombre} (cap. ${e.capacidad})`), visible: v => v.ubicacion === 'ESPACIO' },
        { name: 'reservaId', label: 'Reserva', type: 'select', numerico: true, options: () => reservasDe(inicial.clienteId || (ficha && ficha.id)), visible: v => v.ubicacion === 'RESERVA' },
        { name: 'lugar', label: 'Lugar / direccion', maxlength: 200, visible: v => v.ubicacion === 'EXTERNO' },
        { name: 'menu', label: 'Indicaciones del menu', type: 'textarea', full: true, maxlength: 500 }
      ].filter(Boolean),
      extra: fx => { formRef = fx; return total; },
      onGuardar: async v => {
        const body = { clienteId: v.clienteId ?? null, servicioId: v.servicioId, numeroAsistentes: v.numeroAsistentes, menu: v.menu,
          fecha: v.fecha, horaInicio: v.horaInicio, horaFin: v.horaFin,
          lugar: v.ubicacion === 'EXTERNO' ? v.lugar : null,
          espacioId: v.ubicacion === 'ESPACIO' ? v.espacioId : null, reservaId: v.ubicacion === 'RESERVA' ? v.reservaId : null };
        await api('/solicitudes-catering' + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body });
        toast(fila ? 'Solicitud actualizada' : 'Solicitud registrada (PENDIENTE)', 'ok');
        lista.recargar();
      }
    });
    setTimeout(() => calcular(f), 800);
  }
  if (p.servicio || p.reserva) formulario(null, p);
}
