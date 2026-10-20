/* Pantalla de inicio por rol y reportes. */

function kpi(etiqueta, valor, ir) {
  return h('div', { class: 'card kpi', style: ir ? 'cursor:pointer' : '', onclick: ir ? () => irA(ir) : null },
    h('div', { class: 'etiqueta' }, etiqueta), h('div', { class: 'valor' }, valor));
}
async function total(endpoint, query) {
  try { return (await api(endpoint, { query: { ...query, size: 1 } })).totalElements; } catch (e) { return '-'; }
}

async function vistaInicio(cont) {
  const u = Session.user;
  encabezado(cont, 'Hola, ' + u.nombre.split(' ')[0], 'Panel de ' + fmt.enum(u.rol));
  const grid = h('div', { class: 'grid grid-4' });
  cont.appendChild(grid);

  if (esStaff()) {
    const [ins, res, cat, cot] = await Promise.all([total('/inscripciones', { estado: 'PENDIENTE' }), total('/reservas', { estado: 'SOLICITADA' }),
      total('/solicitudes-catering', { estado: 'PENDIENTE' }), total('/cotizaciones', { estado: 'PENDIENTE' })]);
    grid.append(kpi('Inscripciones por confirmar', ins, 'inscripciones'), kpi('Reservas por confirmar', res, 'reservas'),
      kpi('Catering por confirmar', cat, 'catering'), kpi('Cotizaciones nuevas', cot, 'cotizaciones'));
    await agendaDeHoy(cont);
  }
  if (puede(R.FINANZAS)) {
    try {
      const f = await api('/reportes/financiero');
      const g2 = h('div', { class: 'grid grid-4', style: 'margin-top:1rem' },
        kpi('Recaudado este mes', fmt.dinero(f.recaudado), 'reportes'), kpi('Facturado este mes', fmt.dinero(f.facturado), 'reportes'),
        kpi('Pendiente de cobro', fmt.dinero(f.pendienteDeCobro), 'pagos'), kpi('Pagos por validar', f.abonosPorValidar, 'abonos'));
      cont.appendChild(g2);
    } catch (e) { cont.appendChild(alertaError(e)); }
  }
  if (esCliente()) {
    try {
      const ficha = await api('/clientes/me');
      const hst = await api('/clientes/' + ficha.id + '/historial');
      const vig = (m, ks) => ks.reduce((a, k) => a + (m[k] || 0), 0);
      grid.append(kpi('Inscripciones vigentes', vig(hst.inscripciones, ['PENDIENTE', 'CONFIRMADA']), 'inscripciones'),
        kpi('Reservas vigentes', vig(hst.reservas, ['SOLICITADA', 'CONFIRMADA']), 'reservas'),
        kpi('Cotizaciones abiertas', vig(hst.cotizaciones, ['PENDIENTE', 'EN_PROCESO']), 'cotizaciones'),
        kpi('Saldo pendiente', fmt.dinero(hst.saldoPendiente), 'pagos'));
      cont.appendChild(h('div', { class: 'card', style: 'margin-top:1rem' }, h('h2', null, '¿Que quieres hacer?'),
        h('div', { class: 'row' }, boton('Ver cursos', () => irA('cursos'), 'btn-primary'), boton('Ver diplomados', () => irA('diplomados')),
          boton('Reservar un espacio', () => irA('disponibilidad')), boton('Pedir catering', () => irA('catering')),
          boton('Solicitar cotizacion', () => irA('cotizaciones')), boton('Mis pagos', () => irA('pagos')))));
    } catch (e) { cont.appendChild(alertaError(e)); }
  }
}

async function agendaDeHoy(cont) {
  const hoy = hoyISO();
  const card = h('div', { class: 'card', style: 'margin-top:1rem' }, h('h2', null, 'Agenda de hoy'));
  cont.appendChild(card);
  try {
    const acts = await api('/agenda', { query: { desde: hoy, hasta: hoy } });
    card.appendChild(acts.length ? h('table', null, h('tbody', null, acts.map(a => h('tr', null, h('td', null, badgeTipo(a.tipo)),
      h('td', null, fmt.hora(a.inicio) + ' - ' + fmt.hora(a.fin)), h('td', null, a.titulo), h('td', null, a.espacioNombre || '')))))
      : h('div', { class: 'vacio' }, 'No hay actividades programadas para hoy'));
  } catch (e) { card.appendChild(alertaError(e)); }
}

function vistaReportes(cont) {
  encabezado(cont, 'Reportes', esAdmin() ? 'Resumen operativo y estado financiero.' : 'Estado financiero.');
  const tabs = h('div', { class: 'tabs' });
  const cuerpo = h('div');
  cont.append(tabs, cuerpo);
  const pestanas = [esAdmin() ? ['Resumen general', resumen] : null, ['Estado financiero', financiero]].filter(Boolean);
  pestanas.forEach(([t, fn], i) => {
    const b = h('button', { class: 'tab' + (i === 0 ? ' activo' : ''), onclick: () => { tabs.querySelectorAll('.tab').forEach(x => x.classList.remove('activo')); b.classList.add('activo'); fn(); } }, t);
    tabs.appendChild(b);
  });
  pestanas[0][1]();

  const tablaMapa = (titulo, mapa, formato = x => x) => h('div', { class: 'card' }, h('h3', null, titulo),
    h('table', null, h('tbody', null, Object.entries(mapa).map(([k, v]) => h('tr', null, h('td', null, fmt.enum(k)), h('td', { class: 'right' }, formato(v)))))));

  async function resumen() {
    cuerpo.innerHTML = '';
    try {
      const r = await api('/reportes/resumen');
      cuerpo.append(h('div', { class: 'grid grid-4' }, kpi('Clientes activos', r.clientesActivos), kpi('Cursos activos', r.cursosActivos),
        kpi('Diplomados activos', r.diplomadosActivos), kpi('Docentes activos', r.docentesActivos)),
        h('div', { class: 'grid grid-4', style: 'margin-top:1rem' }, tablaMapa('Inscripciones', r.inscripcionesPorEstado), tablaMapa('Reservas', r.reservasPorEstado),
          tablaMapa('Catering', r.cateringPorEstado), tablaMapa('Cotizaciones', r.cotizacionesPorEstado)),
        h('div', { class: 'card', style: 'margin-top:1rem' }, h('h3', null, 'Ofertas vigentes con mas inscritos'),
          h('table', null, h('thead', null, h('tr', null, ['Tipo', 'Oferta', 'Inscritos', 'Cupo', 'Ocupacion'].map(t => h('th', null, t)))),
            h('tbody', null, r.ofertasConMasInscritos.map(o => h('tr', null, h('td', null, fmt.enum(o.tipo)), h('td', null, o.nombre), h('td', null, o.inscritos),
              h('td', null, o.cupoMaximo), h('td', null, Math.round(o.inscritos * 100 / o.cupoMaximo) + '%')))))));
    } catch (e) { cuerpo.appendChild(alertaError(e)); }
  }

  function financiero() {
    cuerpo.innerHTML = '';
    const d = new Date();
    const desde = h('input', { type: 'date', value: isoLocal(new Date(d.getFullYear(), d.getMonth(), 1)).slice(0, 10) });
    const hasta = h('input', { type: 'date', value: hoyISO() });
    const res = h('div');
    cuerpo.append(h('div', { class: 'filtros' }, h('div', { class: 'campo' }, h('label', null, 'Desde'), desde),
      h('div', { class: 'campo' }, h('label', null, 'Hasta'), hasta), h('button', { class: 'btn btn-primary', onclick: cargar }, 'Consultar')), res);
    async function cargar() {
      res.innerHTML = '';
      try {
        const f = await api('/reportes/financiero', { query: { desde: desde.value, hasta: hasta.value } });
        res.append(h('div', { class: 'grid grid-4' }, kpi('Facturado en el periodo', fmt.dinero(f.facturado)), kpi('Recaudado en el periodo', fmt.dinero(f.recaudado)),
          kpi('Pendiente de cobro (hoy)', fmt.dinero(f.pendienteDeCobro)), kpi('En validacion', fmt.dinero(f.enValidacion) + ' (' + f.abonosPorValidar + ')')),
          h('div', { class: 'grid grid-3', style: 'margin-top:1rem' }, tablaMapa('Recaudado por metodo', f.recaudadoPorMetodo, fmt.dinero),
            tablaMapa('Facturado por proceso', f.facturadoPorTipo, fmt.dinero), tablaMapa('Cuentas por estado', f.cuentasPorEstado)));
      } catch (e) { res.appendChild(alertaError(e)); }
    }
    cargar();
  }
}
