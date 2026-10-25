/* Agenda institucional: calendario mensual de todo lo que ocupa tiempo y espacio, y eventos del CFC. */

const COLORES_TIPO = { CURSO: '#1f5fa8', DIPLOMADO: '#6a3db8', EVENTO: '#c9a227', ALQUILER: '#1e7b4c', CATERING: '#c25a1b' };

function vistaAgenda(cont) {
  encabezado(cont, 'Agenda institucional', esCliente()
    ? 'Cursos, diplomados y eventos del CFC, mas tus reservas y pedidos de catering.'
    : 'Cursos, diplomados, eventos, alquileres y catering. Ningun espacio puede tener dos actividades a la vez.',
    esStaff() ? h('button', { class: 'btn btn-primary', onclick: () => formularioEvento(null, cargar) }, '+ Programar evento') : null);
  let mes = new Date(); mes.setDate(1);
  const titulo = h('h2', { style: 'margin:0' });
  const tipo = h('select', null, h('option', { value: '' }, 'Todos los tipos'), op(ENUMS.TipoActividad).map(o => h('option', { value: o.value }, o.label)));
  const espacio = h('select', null, h('option', { value: '' }, 'Todos los espacios'));
  opcionesActivas('/espacios')().then(ops => ops.forEach(o => espacio.appendChild(h('option', { value: o.value }, o.label)))).catch(() => { });
  const cal = h('div', { class: 'cal' });
  const barra = h('div', { class: 'row', style: 'margin-bottom:.8rem' },
    h('button', { class: 'btn', onclick: () => { mes.setMonth(mes.getMonth() - 1); cargar(); } }, '‹'),
    titulo,
    h('button', { class: 'btn', onclick: () => { mes.setMonth(mes.getMonth() + 1); cargar(); } }, '›'),
    h('button', { class: 'btn', onclick: () => { mes = new Date(); mes.setDate(1); cargar(); } }, 'Hoy'),
    h('div', { class: 'spacer' }), tipo, espacio);
  const leyenda = h('div', { class: 'leyenda', style: 'margin-top:.7rem' },
    Object.entries(COLORES_TIPO).map(([k, c]) => h('span', null, h('i', { style: 'background:' + c }), fmt.enum(k))));
  cont.appendChild(h('div', { class: 'card' }, barra, cal, leyenda));
  tipo.onchange = cargar; espacio.onchange = cargar;

  async function cargar() {
    const y = mes.getFullYear(), m = mes.getMonth();
    titulo.textContent = mes.toLocaleDateString('es-SV', { month: 'long', year: 'numeric' });
    const primero = new Date(y, m, 1);
    const inicio = new Date(primero); inicio.setDate(1 - primero.getDay());
    const fin = new Date(inicio); fin.setDate(inicio.getDate() + 41);
    cal.innerHTML = '';
    ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'].forEach(d => cal.appendChild(h('div', { class: 'dia-nombre' }, d)));
    let actividades = [];
    try {
      actividades = await api('/agenda', { query: { desde: isoLocal(inicio).slice(0, 10), hasta: isoLocal(fin).slice(0, 10), tipo: tipo.value, espacioId: espacio.value } });
    } catch (e) { toast(errorTexto(e), 'error'); }
    const hoy = hoyISO();
    for (let i = 0; i < 42; i++) {
      const d = new Date(inicio); d.setDate(inicio.getDate() + i);
      const iso = isoLocal(d).slice(0, 10);
      const delDia = actividades.filter(a => a.inicio.slice(0, 10) <= iso && a.fin.slice(0, 10) >= iso && !(a.fin.slice(0, 10) === iso && a.fin.slice(11, 16) === '00:00' && a.inicio.slice(0, 10) !== iso));
      cal.appendChild(h('div', { class: 'dia' + (d.getMonth() !== m ? ' fuera' : '') + (iso === hoy ? ' hoy' : '') },
        h('div', { class: 'num' }, d.getDate()),
        delDia.slice(0, 4).map(a => h('span', { class: 'evt evt-' + a.tipo, title: a.titulo + ' ' + fmt.rango(a.inicio, a.fin), onclick: () => verActividad(a) },
          fmt.hora(a.inicio) + ' ' + a.titulo.replace(/^\w+: /, ''))),
        delDia.length > 4 ? h('a', { href: '#', class: 'small', onclick: e => { e.preventDefault(); verDia(iso, delDia); } }, '+' + (delDia.length - 4) + ' mas') : null));
    }
  }

  function verDia(iso, lista) {
    modal({
      titulo: 'Actividades del ' + fmt.fecha(iso),
      cuerpo: h('table', null, h('tbody', null, lista.map(a => h('tr', null, h('td', null, badgeTipo(a.tipo)), h('td', null, fmt.rango(a.inicio, a.fin)),
        h('td', null, h('a', { href: '#', onclick: e => { e.preventDefault(); verActividad(a); } }, a.titulo)), h('td', null, a.espacioNombre || '')))))
    });
  }
  cargar();
}

function badgeTipo(t) { return h('span', { class: 'badge', style: `background:${COLORES_TIPO[t]}22;color:${COLORES_TIPO[t]}` }, fmt.enum(t)); }

function verActividad(a) {
  const destino = { CURSO: 'cursos', DIPLOMADO: 'diplomados', ALQUILER: 'reservas', CATERING: 'catering', EVENTO: 'eventos' }[a.tipo];
  modal({
    titulo: a.titulo,
    cuerpo: detalle([['Tipo', badgeTipo(a.tipo)], ['Horario', fmt.rango(a.inicio, a.fin)], ['Espacio', a.espacioNombre || 'Sin espacio del CFC'],
      ['Bloquea el espacio', fmt.bool(a.bloqueaEspacio)], ['Detalle', a.detalle], ['Registro', '#' + a.referenciaId]]),
    acciones: destino && (destino !== 'eventos' || esStaff()) ? [{ texto: 'Ir al modulo', onClick: c => { c(); irA(destino); } }] : []
  });
}

function vistaEventos(cont) {
  encabezado(cont, 'Eventos institucionales', 'Conferencias, graduaciones y ferias del CFC. Si usan un espacio, lo bloquean en la agenda.',
    h('button', { class: 'btn btn-primary', onclick: () => formularioEvento(null, () => lista.recargar()) }, '+ Programar evento'));
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/agenda/eventos', sortInicial: 'inicio,desc',
    filtros: [{ name: 'q', label: 'Buscar', placeholder: 'Titulo' }, { name: 'estado', label: 'Estado', type: 'select', options: op(ENUMS.EstadoEvento) },
      { name: 'desde', label: 'Desde', type: 'date' }],
    columnas: [{ titulo: 'Titulo', sort: 'titulo', render: f => h('div', null, h('b', null, f.titulo), h('div', { class: 'small muted' }, f.responsable)) },
      { titulo: 'Horario', sort: 'inicio', render: f => fmt.rango(f.inicio, f.fin) },
      { titulo: 'Lugar', render: f => f.espacioNombre || f.lugarExterno },
      { titulo: 'Asistentes', campo: 'asistentesEstimados' },
      { titulo: 'Estado', sort: 'estado', render: f => h('div', null, badge(f.estado), f.motivoCancelacion ? h('div', { class: 'small muted' }, f.motivoCancelacion) : null) }],
    acciones: f => f.estado === 'PROGRAMADO' && f.inicio > isoLocal(new Date()) ? [
      boton('Editar', () => formularioEvento(f, () => lista.recargar())),
      boton('Cancelar', async () => {
        const m = await confirmar('¿Cancelar el evento "' + f.titulo + '"? Se liberara el espacio.', { motivo: true, textoBoton: 'Cancelar evento', clase: 'btn-danger' });
        if (m !== null) accion(() => api('/agenda/eventos/' + f.id, { method: 'DELETE', query: { motivo: m } }), 'Evento cancelado', lista.recargar);
      }, 'btn-danger')] : []
  });
}

function formularioEvento(fila, alGuardar) {
  modalFormulario({
    titulo: fila ? 'Reprogramar evento' : 'Programar evento', inicial: fila || { responsable: Session.user.nombre },
    campos: [
      { name: 'titulo', label: 'Titulo', required: true, full: true, maxlength: 150 },
      { name: 'inicio', label: 'Inicio', type: 'datetime-local', required: true },
      { name: 'fin', label: 'Fin', type: 'datetime-local', required: true },
      { name: 'espacioId', label: 'Espacio del CFC', type: 'select', numerico: true, vacio: 'Ninguno (lugar externo o virtual)',
        options: opcionesActivas('/espacios', e => `${e.nombre} (cap. ${e.capacidad})`) },
      { name: 'lugarExterno', label: 'Lugar externo / enlace', maxlength: 200, visible: v => !v.espacioId },
      { name: 'asistentesEstimados', label: 'Asistentes estimados', type: 'number', min: 1 },
      { name: 'responsable', label: 'Responsable', required: true, maxlength: 120 },
      { name: 'descripcion', label: 'Descripcion', type: 'textarea', full: true, maxlength: 500 }
    ],
    onGuardar: async v => {
      await api('/agenda/eventos' + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body: v });
      toast('Evento guardado', 'ok');
      alGuardar();
    }
  });
}
