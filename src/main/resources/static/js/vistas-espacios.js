/* Espacios, disponibilidad (RF09) y servicios de catering. Usa el CRUD generico vistaCatalogo de vistas-catalogos.js. */

function vistaEspacios(cont) {
  vistaCatalogo(cont, 'espacios', {
    titulo: 'Espacios', desc: 'Auditorios, salas, laboratorios y aulas. "Ocupado" se calcula con la agenda.',
    endpoint: '/espacios',
    filtrosExtra: [{ name: 'tipo', label: 'Tipo', type: 'select', options: op(ENUMS.TipoEspacio) },
      { name: 'capacidadMinima', label: 'Capacidad minima', type: 'number' }],
    columnas: [{ titulo: 'Nombre', campo: 'nombre', sort: 'nombre' }, { titulo: 'Tipo', render: f => fmt.enum(f.tipo), sort: 'tipo' },
      { titulo: 'Capacidad', campo: 'capacidad', sort: 'capacidad' }, { titulo: 'Precio/hora', render: f => fmt.dinero(f.precioHora), sort: 'precioHora' },
      { titulo: 'Equipamiento', campo: 'equipamiento' },
      { titulo: '', render: f => f.activo && puede(R.STAFF_CLIENTE) ? boton('Reservar', () => irA('reservas?espacio=' + f.id)) : '' }],
    campos: [{ name: 'nombre', label: 'Nombre', required: true, maxlength: 100 },
      { name: 'tipo', label: 'Tipo', type: 'select', required: true, options: op(ENUMS.TipoEspacio) },
      { name: 'capacidad', label: 'Capacidad (personas)', type: 'number', required: true, min: 1 },
      { name: 'precioHora', label: 'Precio por hora ($)', type: 'number', required: true, min: 0, step: '0.01' },
      { name: 'ubicacion', label: 'Ubicacion', maxlength: 150 },
      { name: 'equipamiento', label: 'Equipamiento', type: 'textarea', full: true, maxlength: 300 }]
  });
}

function vistaServiciosCatering(cont) {
  vistaCatalogo(cont, 'servicios', {
    titulo: 'Servicios de catering', desc: 'Precio por persona. Un cambio de precio no afecta pedidos ya registrados.',
    endpoint: '/servicios-catering', sortInicial: 'tipo,asc',
    filtrosExtra: [{ name: 'tipo', label: 'Tipo', type: 'select', options: op(ENUMS.TipoServicioCatering) }],
    columnas: [{ titulo: 'Tipo', render: f => fmt.enum(f.tipo), sort: 'tipo' }, { titulo: 'Nombre', campo: 'nombre', sort: 'nombre' },
      { titulo: 'Menu base', campo: 'menuBase' }, { titulo: 'Precio/persona', render: f => fmt.dinero(f.precioUnitario), sort: 'precioUnitario' },
      { titulo: '', render: f => f.activo && puede(R.STAFF_CLIENTE) ? boton('Solicitar', () => irA('catering?servicio=' + f.id)) : '' }],
    campos: [{ name: 'tipo', label: 'Tipo', type: 'select', required: true, options: op(ENUMS.TipoServicioCatering) },
      { name: 'nombre', label: 'Nombre', required: true, maxlength: 100 },
      { name: 'precioUnitario', label: 'Precio por persona ($)', type: 'number', required: true, min: 0.01, step: '0.01' },
      { name: 'menuBase', label: 'Menu base', type: 'textarea', full: true, maxlength: 500 }]
  });
}

/* ------------------------------------------------------------------ Disponibilidad */
COLOR_ESTADO.ok.push('DISPONIBLE');
COLOR_ESTADO.err.push('OCUPADO');
function vistaDisponibilidad(cont) {
  encabezado(cont, 'Disponibilidad de espacios', 'Consulta que espacios estan libres en un horario y cuanto costaria el alquiler.');
  const manana = new Date(); manana.setDate(manana.getDate() + 1); manana.setHours(8, 0, 0, 0);
  const fin = new Date(manana); fin.setHours(12);
  const f = crearFormulario([
    { name: 'inicio', label: 'Desde', type: 'datetime-local', required: true },
    { name: 'fin', label: 'Hasta', type: 'datetime-local', required: true },
    { name: 'tipo', label: 'Tipo', type: 'select', options: op(ENUMS.TipoEspacio), vacio: 'Todos' },
    { name: 'capacidadMinima', label: 'Capacidad minima', type: 'number', min: 1 }
  ], { inicio: isoLocal(manana), fin: isoLocal(fin) });
  const res = h('div');
  const card = h('div', { class: 'card' }, f.form, h('div', { class: 'row', style: 'margin-top:.8rem' },
    h('button', { class: 'btn btn-primary', onclick: consultar }, 'Consultar')), h('div', { style: 'margin-top:1rem' }, res));
  cont.appendChild(card);

  async function consultar() {
    res.innerHTML = '';
    try {
      const datos = await api('/espacios/disponibilidad', { query: f.valores() });
      res.appendChild(h('table', null, h('thead', null, h('tr', null, ['Espacio', 'Tipo', 'Capacidad', 'Costo estimado', 'Estado', ''].map(t => h('th', null, t)))),
        h('tbody', null, datos.map(d => h('tr', null, h('td', null, d.nombre), h('td', null, fmt.enum(d.tipo)), h('td', null, d.capacidad),
          h('td', null, fmt.dinero(d.costoEstimado)),
          h('td', null, d.libre ? badge('DISPONIBLE') : h('span', null, badge('OCUPADO'), h('div', { class: 'small muted' }, d.conflicto))),
          h('td', null, d.libre ? boton('Reservar', () => {
            const v = f.valores();
            irA(`reservas?espacio=${d.espacioId}&inicio=${encodeURIComponent(v.inicio)}&fin=${encodeURIComponent(v.fin)}`);
          }, 'btn-primary') : ''))))));
    } catch (e) { res.appendChild(alertaError(e)); }
  }
  consultar();
}

