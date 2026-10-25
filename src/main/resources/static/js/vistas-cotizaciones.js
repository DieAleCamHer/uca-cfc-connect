/* Cotizaciones con lineas que referencian el catalogo. El precio sugerido sale del catalogo;
   solo el personal puede negociar precio, descuento y vigencia. Aprobar es exclusivo del ADMIN. */

const ITEMS_POR_TIPO = {
  CURSO_EMPRESARIAL: ['CURSO', 'OTRO'], DIPLOMADO: ['DIPLOMADO', 'OTRO'], ALQUILER_ESPACIO: ['ESPACIO', 'OTRO'],
  CATERING: ['CATERING', 'OTRO'], SERVICIO_COMBINADO: ['CURSO', 'DIPLOMADO', 'ESPACIO', 'CATERING', 'OTRO']
};
const CANTIDAD_ITEM = { CURSO: 'Participantes', DIPLOMADO: 'Participantes', ESPACIO: 'Horas', CATERING: 'Asistentes', OTRO: 'Unidades' };

let CATALOGO_COT = null;
async function catalogoCotizacion() {
  if (CATALOGO_COT) return CATALOGO_COT;
  const vig = o => ['PROGRAMADA', 'EN_CURSO'].includes(o.estado);
  const [cursos, diplomados, espacios, servicios] = await Promise.all([
    apiTodos('/cursos', { sort: 'nombre,asc' }), apiTodos('/diplomados', { sort: 'nombre,asc' }),
    apiTodos('/espacios', { activo: true, sort: 'nombre,asc' }), apiTodos('/servicios-catering', { activo: true })]);
  CATALOGO_COT = {
    CURSO: cursos.filter(vig).map(c => ({ id: c.id, label: c.nombre + ' (' + fmt.fecha(c.inicio) + ')', precio: c.costo })),
    DIPLOMADO: diplomados.filter(vig).map(c => ({ id: c.id, label: c.nombre + ' (' + fmt.fecha(c.inicio) + ')', precio: c.costo })),
    ESPACIO: espacios.map(e => ({ id: e.id, label: e.nombre + ' (/hora)', precio: e.precioHora })),
    CATERING: servicios.map(s => ({ id: s.id, label: fmt.enum(s.tipo) + ': ' + s.nombre + ' (/persona)', precio: s.precioUnitario })),
    OTRO: []
  };
  return CATALOGO_COT;
}

function vistaCotizaciones(cont) {
  encabezado(cont, esCliente() ? 'Mis cotizaciones' : 'Cotizaciones',
    'PENDIENTE -> EN PROCESO (el CFC la elabora) -> APROBADA (ADMIN; genera la cuenta) o RECHAZADA. Vencidas se rechazan solas.',
    h('button', { class: 'btn btn-primary', onclick: () => formulario(null) }, esCliente() ? '+ Solicitar cotizacion' : '+ Nueva cotizacion'));
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/cotizaciones', sortInicial: 'id,desc',
    filtros: [{ name: 'q', label: 'Buscar', placeholder: 'Descripcion' },
      { name: 'tipo', label: 'Tipo', type: 'select', options: op(ENUMS.TipoCotizacion) },
      { name: 'estado', label: 'Estado', type: 'select', options: op(ENUMS.EstadoCotizacion) },
      { name: 'desde', label: 'Desde', type: 'date' }],
    columnas: [
      { titulo: '#', campo: 'id', sort: 'id' },
      { titulo: 'Cliente / tipo', sort: 'tipo', render: f => h('div', null, h('b', null, fmt.enum(f.tipo)), esCliente() ? null : h('div', { class: 'small muted' }, f.clienteNombre)) },
      { titulo: 'Descripcion', render: f => h('div', null, f.descripcion, h('div', { class: 'small muted' }, f.items.length + ' linea(s)')) },
      { titulo: 'Solicitada', sort: 'fechaSolicitud', render: f => fmt.fecha(f.fechaSolicitud) },
      { titulo: 'Vence', sort: 'fechaVencimiento', render: f => fmt.fecha(f.fechaVencimiento) },
      { titulo: 'Total', sort: 'total', render: f => fmt.dinero(f.total) },
      { titulo: 'Estado', sort: 'estado', render: f => h('div', null, badge(f.estado), f.motivoRechazo ? h('div', { class: 'small muted' }, f.motivoRechazo) : null) }
    ],
    acciones: f => {
      const a = f.accionesPosibles;
      return [
        boton('Ver', () => detalleCotizacion(f)),
        a.includes('EDITAR') ? boton('Editar', () => formulario(f)) : null,
        a.includes('EN_PROCESO') ? boton('Tomar', () => accion(() => api(`/cotizaciones/${f.id}/en-proceso`, { method: 'PATCH' }), 'En proceso', lista.recargar)) : null,
        a.includes('APROBAR') ? boton('Aprobar', async () => {
          if (await confirmar(`¿Aprobar la cotizacion #${f.id} por ${fmt.dinero(f.total)}? Se generara la cuenta por cobrar.`, { textoBoton: 'Aprobar', clase: 'btn-ok' }) !== null)
            accion(() => api(`/cotizaciones/${f.id}/aprobar`, { method: 'PATCH' }), 'Cotizacion aprobada', lista.recargar);
        }, 'btn-ok') : null,
        a.includes('RECHAZAR') ? boton('Rechazar', async () => {
          const m = await confirmar('Rechazar la cotizacion #' + f.id, { motivo: true, motivoObligatorio: true, textoBoton: 'Rechazar', clase: 'btn-danger' });
          if (m !== null) accion(() => api(`/cotizaciones/${f.id}/rechazar`, { method: 'PATCH', body: { motivo: m } }), 'Rechazada', lista.recargar);
        }, 'btn-danger') : null,
        a.includes('RETIRAR') && esCliente() ? boton('Retirar', async () => {
          const m = await confirmar('¿Retirar tu solicitud de cotizacion #' + f.id + '?', { motivo: true, textoBoton: 'Retirar', clase: 'btn-danger' });
          if (m !== null) accion(() => api('/cotizaciones/' + f.id, { method: 'DELETE', query: { motivo: m } }), 'Retirada', lista.recargar);
        }, 'btn-danger') : null,
        a.includes('ANULAR_APROBACION') ? boton('Anular aprobacion', async () => {
          const m = await confirmar('Anular la aprobacion de #' + f.id + ' (solo si no hay pagos).', { motivo: true, motivoObligatorio: true, textoBoton: 'Anular', clase: 'btn-danger' });
          if (m !== null) accion(() => api(`/cotizaciones/${f.id}/anular-aprobacion`, { method: 'PATCH', body: { motivo: m } }), 'Aprobacion anulada', lista.recargar);
        }, 'btn-danger') : null,
        f.pagoId && typeof detallePago === 'function' ? boton('Cuenta', () => detallePago(f.pagoId)) : null
      ];
    }
  });

  async function formulario(fila) {
    let cat;
    try { cat = await catalogoCotizacion(); } catch (e) { toast(errorTexto(e), 'error'); return; }
    const personal = !esCliente();
    const filas = h('div', { class: 'filas-editor' });
    const resumen = h('div', { class: 'alerta alerta-info', style: 'margin-top:.6rem' });
    let tipoActual = fila ? fila.tipo : 'CURSO_EMPRESARIAL';
    let formulario_;

    function agregarFila(item = {}) {
      const tipo = h('select', null, (ITEMS_POR_TIPO[tipoActual] || []).map(t => h('option', { value: t, selected: t === item.tipoItem }, fmt.enum(t))));
      const ref = h('select');
      const cant = h('input', { type: 'number', min: 1, value: item.cantidad || 1 });
      const precio = h('input', { type: 'number', min: 0, step: '0.01', value: item.precioUnitario ?? '', disabled: !personal, placeholder: 'Catalogo' });
      const desc = h('input', { maxlength: 250, value: item.descripcion || '', placeholder: 'Descripcion (obligatoria en Otro)' });
      const lblCant = h('label', null, CANTIDAD_ITEM[item.tipoItem || tipo.value]);
      function llenarRef() {
        ref.innerHTML = '';
        const ops = cat[tipo.value] || [];
        ref.disabled = tipo.value === 'OTRO';
        if (tipo.value !== 'OTRO') ops.forEach(o => ref.appendChild(h('option', { value: o.id, selected: String(o.id) === String(item.referenciaId) }, o.label + ' - ' + fmt.dinero(o.precio))));
        lblCant.textContent = CANTIDAD_ITEM[tipo.value];
        recalcular();
      }
      tipo.addEventListener('change', () => { item = {}; precio.value = ''; llenarRef(); });
      [ref, cant, precio].forEach(x => x.addEventListener('input', recalcular));
      ref.addEventListener('change', () => { if (personal) precio.value = ''; recalcular(); });
      const fila = h('div', { class: 'fila fila-item' },
        h('div', { class: 'campo' }, h('label', null, 'Tipo'), tipo), h('div', { class: 'campo' }, h('label', null, 'Que'), ref),
        h('div', { class: 'campo' }, lblCant, cant), h('div', { class: 'campo' }, h('label', null, 'Precio unitario'), precio),
        h('div', { class: 'campo' }, h('label', null, 'Descripcion'), desc),
        h('button', { class: 'btn btn-sm btn-danger', type: 'button', onclick: () => { fila.remove(); recalcular(); } }, 'Quitar'));
      fila._leer = () => {
        const t = tipo.value;
        const catalogo = (cat[t] || []).find(o => String(o.id) === ref.value);
        const pu = precio.value !== '' ? Number(precio.value) : (catalogo ? catalogo.precio : 0);
        return {
          enviar: { tipoItem: t, referenciaId: t === 'OTRO' ? null : (ref.value ? Number(ref.value) : null), cantidad: Number(cant.value || 0),
            descripcion: desc.value.trim() || null, precioUnitario: personal && precio.value !== '' ? Number(precio.value) : null },
          subtotal: pu * Number(cant.value || 0)
        };
      };
      filas.appendChild(fila);
      llenarRef();
    }

    function recalcular() {
      const items = [...filas.querySelectorAll('.fila')].map(f => f._leer ? f._leer() : null).filter(Boolean);
      const sub = items.reduce((a, b) => a + b.subtotal, 0);
      const desc = formulario_ ? Number(formulario_.valores().descuento || 0) : Number(fila ? fila.descuento : 0);
      resumen.textContent = `Subtotal ${fmt.dinero(sub)}  -  Descuento ${fmt.dinero(desc)}  =  Total ${fmt.dinero(sub - desc)}` +
        (personal ? '' : '  (precios de catalogo; el CFC puede ajustarlos al elaborarla)');
    }

    const { f } = modalFormulario({
      titulo: fila ? 'Editar cotizacion #' + fila.id : (personal ? 'Nueva cotizacion' : 'Solicitar cotizacion'), grande: true,
      inicial: fila ? { ...fila } : { tipo: tipoActual },
      campos: [
        personal ? { name: 'clienteId', label: 'Cliente', type: 'select', required: true, numerico: true, options: opcionesClientes, readonly: !!fila } : null,
        { name: 'tipo', label: 'Tipo', type: 'select', required: true, options: op(ENUMS.TipoCotizacion),
          onchange: v => { if (v && v !== tipoActual) { tipoActual = v; filas.innerHTML = ''; agregarFila(); } } },
        { name: 'fechaEvento', label: 'Fecha tentativa del servicio', type: 'date' },
        personal ? { name: 'fechaVencimiento', label: 'Vigente hasta', type: 'date', ayuda: 'Por defecto 15 dias' } : null,
        personal ? { name: 'descuento', label: 'Descuento ($)', type: 'number', min: 0, step: '0.01', onchange: () => recalcular() } : null,
        { name: 'descripcion', label: 'Que necesitas', type: 'textarea', required: true, full: true, maxlength: 500 }
      ].filter(Boolean),
      extra: fx => {
        formulario_ = fx;
        return h('div', { style: 'margin-top:1rem' }, h('h3', null, 'Lineas'),
          h('p', { class: 'small muted' }, 'Servicio combinado: al menos dos servicios distintos. "Otro" sirve para cargos libres (transporte, material).'),
          filas, h('button', { class: 'btn btn-sm', type: 'button', onclick: () => agregarFila() }, '+ Agregar linea'), resumen);
      },
      onGuardar: async v => {
        const items = [...filas.querySelectorAll('.fila')].map(x => x._leer().enviar);
        const body = { clienteId: v.clienteId ?? null, tipo: v.tipo, descripcion: v.descripcion, fechaEvento: v.fechaEvento, items,
          descuento: personal ? v.descuento : null, fechaVencimiento: personal ? v.fechaVencimiento : null };
        await api('/cotizaciones' + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body });
        toast(fila ? 'Cotizacion actualizada' : 'Cotizacion registrada', 'ok');
        lista.recargar();
      }
    });
    (fila ? fila.items : [{}]).forEach(i => agregarFila(i));
    recalcular();
    f.form.addEventListener('input', recalcular);
  }
}

function detalleCotizacion(c) {
  modal({
    titulo: 'Cotizacion #' + c.id, grande: true,
    cuerpo: h('div', { class: 'imprimible' },
      detalle([['Cliente', c.clienteNombre], ['Tipo', fmt.enum(c.tipo)], ['Estado', badge(c.estado)], ['Solicitada', fmt.fechaHora(c.fechaSolicitud)],
        ['Fecha del servicio', fmt.fecha(c.fechaEvento)], ['Vigente hasta', fmt.fecha(c.fechaVencimiento)], ['Elaborada por', c.elaboradaPor],
        ['Aprobada por', c.aprobadaPor], c.motivoRechazo ? ['Motivo', c.motivoRechazo] : null]),
      h('p', { style: 'margin-top:1rem' }, c.descripcion),
      h('table', null, h('thead', null, h('tr', null, ['Tipo', 'Descripcion', 'Cantidad', 'Precio', 'Subtotal'].map(t => h('th', null, t)))),
        h('tbody', null, c.items.map(i => h('tr', null, h('td', null, fmt.enum(i.tipoItem)), h('td', null, i.descripcion), h('td', null, i.cantidad),
          h('td', null, fmt.dinero(i.precioUnitario)), h('td', null, fmt.dinero(i.subtotal))))),
        h('tfoot', null,
          h('tr', null, h('td', { colspan: 4, class: 'right' }, 'Subtotal'), h('td', null, fmt.dinero(c.subtotal))),
          h('tr', null, h('td', { colspan: 4, class: 'right' }, 'Descuento'), h('td', null, fmt.dinero(c.descuento))),
          h('tr', null, h('td', { colspan: 4, class: 'right' }, h('b', null, 'Total')), h('td', null, h('b', null, fmt.dinero(c.total))))))),
    acciones: [{ texto: 'Imprimir', onClick: () => window.print() }]
  });
}
