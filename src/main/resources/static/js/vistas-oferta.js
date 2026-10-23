/* Cursos y diplomados: listado con filtros, formulario con editor de sesiones y generador semanal. */

const opcionesActivas = (endpoint, etiqueta) => async () =>
  (await apiTodos(endpoint, { activo: true, sort: 'nombre,asc' })).map(x => ({ value: x.id, label: etiqueta ? etiqueta(x) : x.nombre }));

function vistaOferta(cont, recurso) {
  const singular = recurso === 'cursos' ? 'curso' : 'diplomado';
  const Titulo = recurso === 'cursos' ? 'Cursos' : 'Diplomados';
  const crear = esStaff() ? h('button', { class: 'btn btn-primary', onclick: () => formularioOferta(recurso, null, () => lista.recargar()) }, '+ Nuevo ' + singular) : null;
  encabezado(cont, Titulo, 'Oferta con sesiones, cupo y costo. Presencial ocupa su espacio en la agenda una vez por sesion.', crear);
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/' + recurso, sortInicial: 'inicio,asc',
    filtrosIniciales: esCliente() ? { conCupo: 'true' } : {},
    filtros: [
      { name: 'q', label: 'Buscar', placeholder: 'Nombre o descripcion' },
      { name: 'categoriaId', label: 'Categoria', type: 'select', options: opcionesActivas('/categorias') },
      { name: 'modalidadId', label: 'Modalidad', type: 'select', options: opcionesActivas('/modalidades') },
      esCliente() ? null : { name: 'docenteId', label: 'Docente', type: 'select', options: opcionesActivas('/docentes') },
      { name: 'estado', label: 'Estado', type: 'select', options: op(esCliente() ? ['PROGRAMADA', 'EN_CURSO'] : ENUMS.EstadoOferta) },
      { name: 'desde', label: 'Desde', type: 'date' },
      { name: 'conCupo', label: 'Cupo', type: 'select', options: [{ value: 'true', label: 'Con cupo disponible' }] }
    ].filter(Boolean),
    columnas: [
      { titulo: 'Nombre', sort: 'nombre', render: f => h('div', null, h('b', null, f.nombre), h('div', { class: 'small muted' }, f.categoriaNombre)) },
      { titulo: 'Modalidad', sort: 'modalidad', render: f => h('div', null, f.modalidadNombre, f.espacioNombre ? h('div', { class: 'small muted' }, f.espacioNombre) : null) },
      { titulo: 'Docente', sort: 'docente', campo: 'docenteNombre' },
      { titulo: 'Fechas', sort: 'inicio', render: f => h('div', null, fmt.fecha(f.inicio) + ' - ' + fmt.fecha(f.fin), h('div', { class: 'small muted' }, f.sesiones.length + ' sesion(es), ' + f.horasTotales + ' h')) },
      { titulo: 'Cupo', sort: 'cupoMaximo', render: f => `${f.inscritos}/${f.cupoMaximo}` },
      { titulo: 'Costo', sort: 'costo', render: f => fmt.dinero(f.costo) },
      { titulo: 'Estado', render: f => badge(f.estado) }
    ],
    acciones: f => [
      boton('Ver', () => detalleOferta(recurso, f)),
      puede(R.STAFF_CLIENTE) && ['PROGRAMADA', 'EN_CURSO'].includes(f.estado) && f.cupoDisponible > 0
        ? boton('Inscribir', () => irA('inscripciones?' + singular + '=' + f.id), 'btn-primary') : null,
      esStaff() && ['PROGRAMADA', 'EN_CURSO'].includes(f.estado) ? boton('Editar', () => formularioOferta(recurso, f, () => lista.recargar())) : null,
      esAdmin() && f.activo ? boton('Desactivar', async () => {
        const ok = await confirmar(`¿Desactivar "${f.nombre}"? Si aun no termina se cancelaran sus ${f.inscritos} inscripcion(es) vigentes, `
          + 'se anularan sus cuentas y se liberara la agenda. Si alguna tiene dinero cobrado no se hara nada.', { textoBoton: 'Desactivar', clase: 'btn-danger' });
        if (ok !== null) accion(() => api(`/${recurso}/${f.id}`, { method: 'DELETE' }), 'Desactivado', lista.recargar);
      }, 'btn-danger') : null,
      esAdmin() && !f.activo ? boton('Reactivar', () => accion(() => api(`/${recurso}/${f.id}/activar`, { method: 'PATCH' }), 'Reactivado', lista.recargar)) : null
    ]
  });
}

function detalleOferta(recurso, f) {
  modal({
    titulo: f.nombre, grande: true,
    cuerpo: h('div', null,
      f.descripcion ? h('p', null, f.descripcion) : null,
      detalle([['Tipo', fmt.enum(f.tipo)], ['Estado', badge(f.estado)], ['Categoria', f.categoriaNombre], ['Modalidad', f.modalidadNombre],
        ['Docente', f.docenteNombre], ['Espacio', f.espacioNombre], ['Cupo', `${f.inscritos} inscritos de ${f.cupoMaximo} (${f.cupoDisponible} libres)`],
        ['Costo', fmt.dinero(f.costo)], ['Duracion', f.horasTotales + ' horas'], ['Periodo', fmt.fecha(f.inicio) + ' - ' + fmt.fecha(f.fin)]]),
      h('h3', { style: 'margin-top:1rem' }, 'Sesiones'),
      h('table', null, h('thead', null, h('tr', null, h('th', null, '#'), h('th', null, 'Horario'), h('th', null, 'Horas'))),
        h('tbody', null, f.sesiones.map(s => h('tr', null, h('td', null, s.numero), h('td', null, fmt.rango(s.inicio, s.fin)), h('td', null, s.horas))))))
  });
}

/** Editor de sesiones: filas inicio/fin + generador semanal. */
function editorSesiones(iniciales) {
  const filas = h('div', { class: 'filas-editor' });
  function agregar(ini = '', fin = '') {
    const a = h('input', { type: 'datetime-local', value: ini ? ini.slice(0, 16) : '' });
    const b = h('input', { type: 'datetime-local', value: fin ? fin.slice(0, 16) : '' });
    a.addEventListener('change', () => {
      if (a.value && !b.value) { const d = new Date(a.value); d.setHours(d.getHours() + 2); b.value = isoLocal(d); }
    });
    const fila = h('div', { class: 'fila fila-sesion' },
      h('div', { class: 'campo' }, h('label', null, 'Inicio'), a), h('div', { class: 'campo' }, h('label', null, 'Fin'), b),
      h('button', { class: 'btn btn-sm btn-danger', type: 'button', onclick: () => fila.remove() }, 'Quitar'));
    filas.appendChild(fila);
  }
  (iniciales && iniciales.length ? iniciales : [{}]).forEach(s => agregar(s.inicio, s.fin));

  // Generador semanal
  const g = {
    desde: h('input', { type: 'date' }), hasta: h('input', { type: 'date' }),
    hi: h('input', { type: 'time', value: '18:00' }), hf: h('input', { type: 'time', value: '20:00' })
  };
  const dias = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'].map((d, i) => ({ i, chk: h('input', { type: 'checkbox' }), d }));
  const generador = h('details', { class: 'caja', style: 'margin-top:.6rem' }, h('summary', null, 'Generar sesiones semanales'),
    h('div', { class: 'form-grid', style: 'margin-top:.6rem' },
      h('div', { class: 'campo' }, h('label', null, 'Primer dia'), g.desde), h('div', { class: 'campo' }, h('label', null, 'Ultimo dia'), g.hasta),
      h('div', { class: 'campo' }, h('label', null, 'Hora inicio'), g.hi), h('div', { class: 'campo' }, h('label', null, 'Hora fin'), g.hf),
      h('div', { class: 'campo full' }, h('label', null, 'Dias'), h('div', { class: 'row' }, dias.map(x => h('label', { class: 'row' }, x.chk, x.d))))),
    h('button', {
      class: 'btn btn-sm', type: 'button', style: 'margin-top:.6rem', onclick: () => {
        if (!g.desde.value || !g.hasta.value) { toast('Indica el primer y ultimo dia', 'error'); return; }
        const marcados = dias.filter(x => x.chk.checked).map(x => x.i);
        if (!marcados.length) { toast('Marca al menos un dia', 'error'); return; }
        const d = new Date(g.desde.value + 'T12:00'), fin = new Date(g.hasta.value + 'T12:00');
        let n = 0;
        [...filas.querySelectorAll('.fila')].forEach(f => { const i = f.querySelectorAll('input'); if (!i[0].value && !i[1].value) f.remove(); });
        while (d <= fin && n < 200) {
          if (marcados.includes(d.getDay())) {
            const fecha = isoLocal(d).slice(0, 10);
            let fechaFin = fecha;
            if (g.hf.value <= g.hi.value) { const x = new Date(d); x.setDate(x.getDate() + 1); fechaFin = isoLocal(x).slice(0, 10); }
            agregar(fecha + 'T' + g.hi.value, fechaFin + 'T' + g.hf.value); n++;
          }
          d.setDate(d.getDate() + 1);
        }
        toast(n + ' sesion(es) generadas', 'ok');
      }
    }, 'Generar'));
  const nodo = h('div', { class: 'campo full' }, h('label', null, 'Sesiones ', h('span', { class: 'req' }, '*')),
    h('span', { class: 'ayuda' }, 'Cada sesion dura de 30 min a 12 h; si termina de madrugada el fin es del dia siguiente.'),
    filas, h('button', { class: 'btn btn-sm', type: 'button', onclick: () => agregar() }, '+ Agregar sesion'), generador,
    h('span', { class: 'error-campo', 'data-sesiones': '1' }));
  return {
    nodo,
    valores: () => [...filas.querySelectorAll('.fila')].map(f => { const i = f.querySelectorAll('input'); return { inicio: i[0].value || null, fin: i[1].value || null }; })
      .filter(s => s.inicio || s.fin)
  };
}

async function formularioOferta(recurso, fila, alGuardar) {
  const singular = recurso === 'cursos' ? 'curso' : 'diplomado';
  const requiere = {};
  const modalidades = async () => (await apiTodos('/modalidades', { activo: true, sort: 'nombre,asc' })).map(m => {
    requiere[m.id] = m.requiereEspacioFisico;
    return { value: m.id, label: m.nombre + (m.requiereEspacioFisico ? ' (requiere espacio)' : ' (sin espacio)') };
  });
  const sesiones = editorSesiones(fila ? fila.sesiones : []);
  const iniciado = fila && fila.estado === 'EN_CURSO';
  const { f } = modalFormulario({
    titulo: (fila ? 'Editar ' : 'Nuevo ') + singular, grande: true,
    inicial: fila || {},
    campos: [
      { name: 'nombre', label: 'Nombre', required: true, maxlength: 150 },
      { name: 'categoriaId', label: 'Categoria', type: 'select', required: true, numerico: true, options: opcionesActivas('/categorias'), readonly: iniciado },
      { name: 'modalidadId', label: 'Modalidad', type: 'select', required: true, numerico: true, options: modalidades, readonly: iniciado },
      { name: 'docenteId', label: 'Docente', type: 'select', required: true, numerico: true, readonly: iniciado,
        options: opcionesActivas('/docentes', d => d.nombre + (d.especialidad ? ' - ' + d.especialidad : '')) },
      { name: 'espacioId', label: 'Espacio', type: 'select', numerico: true, readonly: iniciado,
        options: opcionesActivas('/espacios', e => `${e.nombre} (${fmt.enum(e.tipo)}, cap. ${e.capacidad})`),
        visible: v => !!requiere[v.modalidadId], ayuda: 'Obligatorio en modalidad presencial; el cupo no puede superar su capacidad' },
      { name: 'cupoMaximo', label: 'Cupo maximo', type: 'number', required: true, min: 1, max: 500 },
      { name: 'costo', label: 'Costo por participante ($)', type: 'number', required: true, min: 0, step: '0.01' },
      { name: 'descripcion', label: 'Descripcion', type: 'textarea', full: true, maxlength: 500 }
    ],
    extra: () => h('div', null, iniciado ? h('div', { class: 'alerta alerta-info', style: 'margin-top:1rem' },
      'Ya inicio: solo se pueden cambiar nombre, descripcion, cupo y costo.') : null, h('div', { class: 'form-grid', style: 'margin-top:1rem' }, sesiones.nodo)),
    onGuardar: async v => {
      const body = { ...v, sesiones: sesiones.valores() };
      if (!requiere[body.modalidadId]) body.espacioId = null;
      await api('/' + recurso + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body });
      toast('Guardado', 'ok');
      alGuardar();
    }
  });
  // al cargar las modalidades se recalcula la visibilidad del espacio
  setTimeout(() => f.refrescarVisibles(), 600);
}
