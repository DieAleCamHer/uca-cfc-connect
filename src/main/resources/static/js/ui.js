/* Utilidades de interfaz: construccion de elementos, formatos, modales, formularios y listados paginados. */

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** h('div', {class:'x', onclick: fn}, 'texto', otroNodo) -> crea elementos sin innerHTML (evita inyeccion de HTML). */
function h(tag, attrs, ...hijos) {
  const el = document.createElement(tag);
  if (attrs) {
    Object.entries(attrs).forEach(([k, v]) => {
      if (v === undefined || v === null || v === false) return;
      if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
      else if (k === 'class') el.className = v;
      else if (k === 'value') el.value = v;
      else if (v === true) el.setAttribute(k, '');
      else el.setAttribute(k, v);
    });
  }
  hijos.flat(Infinity).forEach(c => {
    if (c === undefined || c === null || c === false) return;
    el.appendChild(c instanceof Node ? c : document.createTextNode(String(c)));
  });
  return el;
}

const ETIQUETAS = {
  ADMIN: 'Administrador', RECEPCIONISTA: 'Recepcionista', CLIENTE: 'Cliente', CONTABILIDAD: 'Contabilidad',
  EN_PROCESO: 'En proceso', PENDIENTE_VALIDACION: 'En validacion', SALA_REUNIONES: 'Sala de reuniones',
  SALA_MULTIMEDIA: 'Sala multimedia', COFFEE_BREAK: 'Coffee break', CURSO_EMPRESARIAL: 'Curso empresarial',
  ALQUILER_ESPACIO: 'Alquiler de espacio', SERVICIO_COMBINADO: 'Servicio combinado', EN_CURSO: 'En curso'
};

const fmt = {
  enum(v) { if (!v) return ''; return ETIQUETAS[v] || (v.charAt(0) + v.slice(1).toLowerCase().replace(/_/g, ' ')); },
  fecha(v) { if (!v) return ''; const [y, m, d] = v.slice(0, 10).split('-'); return `${d}/${m}/${y}`; },
  hora(v) { if (!v) return ''; return v.length > 8 ? v.slice(11, 16) : v.slice(0, 5); },
  fechaHora(v) { return v ? fmt.fecha(v) + ' ' + fmt.hora(v) : ''; },
  rango(a, b) {
    if (!a) return '';
    return a.slice(0, 10) === (b || '').slice(0, 10) ? `${fmt.fecha(a)} ${fmt.hora(a)}-${fmt.hora(b)}` : `${fmt.fechaHora(a)} -> ${fmt.fechaHora(b)}`;
  },
  dinero(v) { if (v === null || v === undefined || v === '') return ''; return '$' + Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
  bool(v) { return v ? 'Si' : 'No'; }
};

/** ISO local (sin zona) para inputs datetime-local y para el backend. */
function isoLocal(d) {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}
function hoyISO() { return isoLocal(new Date()).slice(0, 10); }

const COLOR_ESTADO = {
  ok: ['CONFIRMADA', 'APROBADA', 'PAGADO', 'VALIDADO', 'COMPLETADA', 'FINALIZADA', 'ATENDIDA', 'PROGRAMADA', 'PROGRAMADO', 'ACTIVO'],
  warn: ['PENDIENTE', 'SOLICITADA', 'PARCIAL', 'EN_PROCESO', 'PENDIENTE_VALIDACION', 'EN_CURSO'],
  err: ['CANCELADA', 'RECHAZADA', 'RECHAZADO', 'ANULADO', 'CANCELADO', 'INACTIVA', 'INACTIVO'],
};
function badge(estado) {
  if (estado === true) estado = 'ACTIVO';
  if (estado === false) estado = 'INACTIVO';
  let cls = 'badge';
  if (COLOR_ESTADO.ok.includes(estado)) cls += ' b-ok';
  else if (COLOR_ESTADO.warn.includes(estado)) cls += ' b-warn';
  else if (COLOR_ESTADO.err.includes(estado)) cls += ' b-err';
  else cls += ' b-info';
  return h('span', { class: cls }, fmt.enum(estado));
}

function toast(msg, tipo = '') {
  let cont = document.getElementById('toasts');
  if (!cont) { cont = h('div', { id: 'toasts' }); document.body.appendChild(cont); }
  const t = h('div', { class: 'toast ' + tipo }, msg);
  cont.appendChild(t);
  setTimeout(() => t.remove(), tipo === 'error' ? 7000 : 3500);
}

function errorTexto(e) { return e instanceof ApiError ? e.texto : (e && e.message) || String(e); }
function alertaError(e) {
  const det = e instanceof ApiError && e.details.length ? h('ul', null, e.details.map(d => h('li', null, d))) : null;
  return h('div', { class: 'alerta alerta-error' }, e.message || String(e), det);
}

/** modal({titulo, cuerpo: Node, acciones: [{texto, clase, onClick(cerrar)}], grande}) */
function modal({ titulo, cuerpo, acciones = [], grande = false, alCerrar }) {
  const fondo = h('div', { class: 'modal-fondo' });
  const cerrar = () => { fondo.remove(); if (alCerrar) alCerrar(); };
  const foot = h('div', { class: 'modal-foot' },
    acciones.map(a => h('button', { class: 'btn ' + (a.clase || ''), type: 'button', onclick: () => a.onClick(cerrar) }, a.texto)),
    h('button', { class: 'btn', type: 'button', onclick: cerrar }, 'Cerrar'));
  const caja = h('div', { class: 'modal' + (grande ? ' grande' : '') },
    h('div', { class: 'modal-head' }, h('h2', null, titulo), h('button', { class: 'cerrar', onclick: cerrar, 'aria-label': 'Cerrar' }, '×')),
    h('div', { class: 'modal-body' }, cuerpo), foot);
  fondo.appendChild(caja);
  fondo.addEventListener('mousedown', ev => { if (ev.target === fondo) cerrar(); });
  document.body.appendChild(fondo);
  return { cerrar, caja, foot };
}

/** Confirmacion con motivo opcional u obligatorio. Devuelve null si cancela, o el texto del motivo. */
function confirmar(mensaje, { motivo = false, motivoObligatorio = false, textoBoton = 'Confirmar', clase = 'btn-primary' } = {}) {
  return new Promise(resolve => {
    const input = motivo ? h('textarea', { maxlength: 300, placeholder: motivoObligatorio ? 'Motivo (obligatorio)' : 'Motivo (opcional)' }) : null;
    const error = h('div', { class: 'error-campo' });
    let resuelto = false;
    const m = modal({
      titulo: 'Confirmar',
      cuerpo: h('div', null, h('p', null, mensaje), input, error),
      acciones: [{
        texto: textoBoton, clase, onClick: cerrar => {
          const v = input ? input.value.trim() : '';
          if (motivoObligatorio && !v) { error.textContent = 'El motivo es obligatorio'; return; }
          resuelto = true; cerrar(); resolve(v);
        }
      }],
      alCerrar: () => { if (!resuelto) resolve(null); }
    });
    if (input) input.focus();
  });
}

/* ------------------------------------------------------------------ Formularios declarativos */
/**
 * campos: [{ name, label, type: text|number|email|password|date|time|datetime-local|select|textarea|checkbox,
 *            required, options: [{value,label}] | async () => [...], ayuda, full, min, max, step, placeholder,
 *            visible: (valores) => bool, onchange: (valor, form) => void }]
 */
function crearFormulario(campos, inicial = {}) {
  const form = h('form', { class: 'form-grid', novalidate: true });
  const errorGeneral = h('div');
  const controles = {};
  const wrappers = {};

  campos.forEach(c => {
    let ctrl;
    if (c.type === 'select') {
      ctrl = h('select', { name: c.name });
    } else if (c.type === 'textarea') {
      ctrl = h('textarea', { name: c.name, maxlength: c.maxlength, placeholder: c.placeholder });
    } else if (c.type === 'checkbox') {
      ctrl = h('input', { type: 'checkbox', name: c.name });
    } else {
      ctrl = h('input', { type: c.type || 'text', name: c.name, min: c.min, max: c.max, step: c.step, placeholder: c.placeholder, maxlength: c.maxlength });
    }
    if (c.readonly) ctrl.disabled = true;
    controles[c.name] = ctrl;
    const label = h('label', null, c.label, c.required ? h('span', { class: 'req' }, ' *') : null);
    const w = h('div', { class: 'campo' + (c.full ? ' full' : '') + (c.type === 'checkbox' ? ' checkbox' : '') },
      c.type === 'checkbox' ? [ctrl, label] : [label, ctrl], c.ayuda ? h('span', { class: 'ayuda' }, c.ayuda) : null,
      h('span', { class: 'error-campo' }));
    wrappers[c.name] = w;
    form.appendChild(w);
    ctrl.addEventListener('change', () => { if (c.onchange) c.onchange(leerValor(c), api_); refrescarVisibles(); });
  });

  function leerValor(c) {
    const ctrl = controles[c.name];
    if (c.type === 'checkbox') return ctrl.checked;
    const v = ctrl.value;
    if (v === '') return null;
    if (c.type === 'number') return Number(v);
    if (c.numerico) return Number(v);
    return v;
  }

  function valores() {
    const out = {};
    campos.forEach(c => { if (!wrappers[c.name].classList.contains('hidden')) out[c.name] = leerValor(c); else out[c.name] = null; });
    return out;
  }

  function refrescarVisibles() {
    const v = {};
    campos.forEach(c => { v[c.name] = leerValor(c); });
    campos.forEach(c => { if (c.visible) wrappers[c.name].classList.toggle('hidden', !c.visible(v)); });
  }

  async function setOpciones(name, opciones, valorActual) {
    const ctrl = controles[name];
    const c = campos.find(x => x.name === name);
    ctrl.innerHTML = '';
    if (!c.required || c.vacio) ctrl.appendChild(h('option', { value: '' }, c.vacio || '-- Seleccione --'));
    else ctrl.appendChild(h('option', { value: '' }, '-- Seleccione --'));
    opciones.forEach(o => ctrl.appendChild(h('option', { value: o.value, disabled: o.disabled }, o.label)));
    if (valorActual !== undefined && valorActual !== null && valorActual !== '') {
      // Al editar, el valor actual puede estar desactivado y no venir en la lista: se agrega para no perderlo.
      if (!opciones.some(o => String(o.value) === String(valorActual))) {
        ctrl.appendChild(h('option', { value: valorActual }, (c.etiquetaActual ? c.etiquetaActual(valorActual) : '#' + valorActual) + ' (actual)'));
      }
      ctrl.value = String(valorActual);
    }
  }

  function setValor(name, v) {
    const c = campos.find(x => x.name === name);
    if (!c) return;
    if (c.type === 'checkbox') controles[name].checked = !!v;
    else controles[name].value = v === null || v === undefined ? '' : (c.type === 'datetime-local' && v ? String(v).slice(0, 16) : (c.type === 'time' && v ? String(v).slice(0, 5) : v));
  }

  function mostrarError(e) {
    Object.values(wrappers).forEach(w => { w.classList.remove('invalido'); w.querySelector('.error-campo').textContent = ''; });
    errorGeneral.innerHTML = '';
    if (!e) return;
    const restantes = [];
    (e.details || []).forEach(d => {
      const [campo, ...msg] = d.split(': ');
      const base = campo.split(/[.[]/)[0];
      if (wrappers[base]) { wrappers[base].classList.add('invalido'); wrappers[base].querySelector('.error-campo').textContent = msg.join(': '); }
      else restantes.push(d);
    });
    errorGeneral.appendChild(h('div', { class: 'alerta alerta-error' }, e.message,
      restantes.length ? h('ul', null, restantes.map(r => h('li', null, r))) : null));
  }

  const api_ = { form, errorGeneral, controles, wrappers, valores, setOpciones, setValor, mostrarError, refrescarVisibles };

  (async () => {
    for (const c of campos) {
      if (c.type === 'select' && c.options) {
        try {
          const ops = typeof c.options === 'function' ? await c.options() : c.options;
          await setOpciones(c.name, ops, inicial[c.name]);
        } catch (e) { toast('No se pudieron cargar las opciones de ' + c.label + ': ' + errorTexto(e), 'error'); }
      } else if (inicial[c.name] !== undefined) {
        setValor(c.name, inicial[c.name]);
      }
    }
    refrescarVisibles();
    campos.forEach(c => { if (c.onchange && inicial[c.name] !== undefined) c.onchange(leerValor(c), api_); });
  })();
  return api_;
}

/** Abre un modal con formulario y llama a onGuardar(valores). Si lanza ApiError, lo muestra en el formulario. */
function modalFormulario({ titulo, campos, inicial = {}, onGuardar, textoGuardar = 'Guardar', grande = false, extra }) {
  const f = crearFormulario(campos, inicial);
  const cuerpo = h('div', null, f.errorGeneral, f.form, extra ? extra(f) : null);
  const m = modal({
    titulo, cuerpo, grande,
    acciones: [{
      texto: textoGuardar, clase: 'btn-primary', onClick: async cerrar => {
        f.mostrarError(null);
        try { await onGuardar(f.valores(), f); cerrar(); } catch (e) { f.mostrarError(e); }
      }
    }]
  });
  return { f, m };
}

const ENUMS = {
  TipoCliente: ['PERSONA', 'EMPRESA'],
  TipoEspacio: ['AUDITORIO', 'SALA_REUNIONES', 'LABORATORIO', 'AULA', 'SALA_MULTIMEDIA'],
  TipoServicioCatering: ['COFFEE_BREAK', 'DESAYUNO', 'ALMUERZO', 'CENA', 'REFRIGERIO'],
  Rol: ['ADMIN', 'RECEPCIONISTA', 'CLIENTE', 'CONTABILIDAD'],
  RolPersonal: ['ADMIN', 'RECEPCIONISTA', 'CONTABILIDAD'],
  EstadoOferta: ['PROGRAMADA', 'EN_CURSO', 'FINALIZADA', 'INACTIVA'],
  EstadoInscripcion: ['PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA'],
  EstadoReserva: ['SOLICITADA', 'CONFIRMADA', 'COMPLETADA', 'RECHAZADA', 'CANCELADA'],
  EstadoSolicitudCatering: ['PENDIENTE', 'CONFIRMADA', 'ATENDIDA', 'RECHAZADA', 'CANCELADA'],
  EstadoCotizacion: ['PENDIENTE', 'EN_PROCESO', 'APROBADA', 'RECHAZADA'],
  TipoCotizacion: ['CURSO_EMPRESARIAL', 'DIPLOMADO', 'ALQUILER_ESPACIO', 'CATERING', 'SERVICIO_COMBINADO'],
  TipoItem: ['CURSO', 'DIPLOMADO', 'ESPACIO', 'CATERING', 'OTRO'],
  EstadoPago: ['PENDIENTE', 'PARCIAL', 'PAGADO', 'ANULADO'],
  EstadoAbono: ['PENDIENTE_VALIDACION', 'VALIDADO', 'RECHAZADO', 'ANULADO'],
  MetodoPago: ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'DEPOSITO'],
  TipoReferencia: ['INSCRIPCION', 'ALQUILER', 'CATERING', 'COTIZACION'],
  TipoActividad: ['CURSO', 'DIPLOMADO', 'EVENTO', 'ALQUILER', 'CATERING'],
  EstadoEvento: ['PROGRAMADO', 'CANCELADO']
};
const op = lista => lista.map(v => ({ value: v, label: fmt.enum(v) }));
const opBool = [{ value: 'true', label: 'Activos' }, { value: 'false', label: 'Inactivos' }];

/* ------------------------------------------------------------------ Listado paginado generico */
/**
 * listado(contenedor, {
 *   endpoint, columnas: [{titulo, campo, sort, render(fila)}], filtros: [{name,label,type,options}],
 *   acciones: (fila) => [Node], sortInicial, vacio
 * }) -> { recargar() }
 */
function listado(cont, cfg) {
  const estado = { page: 0, size: 10, sort: cfg.sortInicial || '', filtros: { ...(cfg.filtrosIniciales || {}) } };
  const barra = h('div', { class: 'filtros' });
  const tablaWrap = h('div', { class: 'tabla-wrap' });
  const pag = h('div', { class: 'paginacion' });
  const filtroCtrls = {};

  (cfg.filtros || []).forEach(f => {
    let ctrl;
    if (f.type === 'select') {
      ctrl = h('select', null, h('option', { value: '' }, f.todos || 'Todos'));
      const llenar = ops => ops.forEach(o => ctrl.appendChild(h('option', { value: o.value }, o.label)));
      if (typeof f.options === 'function') f.options().then(llenar).catch(() => { });
      else llenar(f.options || []);
    } else {
      ctrl = h('input', { type: f.type || 'text', placeholder: f.placeholder || '' });
    }
    if (estado.filtros[f.name] !== undefined) ctrl.value = estado.filtros[f.name];
    filtroCtrls[f.name] = ctrl;
    const evento = f.type === 'select' || f.type === 'date' ? 'change' : 'input';
    let t;
    ctrl.addEventListener(evento, () => {
      clearTimeout(t);
      t = setTimeout(() => { estado.filtros[f.name] = ctrl.value; estado.page = 0; cargar(); }, evento === 'input' ? 350 : 0);
    });
    barra.appendChild(h('div', { class: 'campo' }, h('label', null, f.label), ctrl));
  });
  if (cfg.filtros && cfg.filtros.length) {
    barra.appendChild(h('button', {
      class: 'btn', onclick: () => {
        Object.entries(filtroCtrls).forEach(([k, c]) => { c.value = ''; estado.filtros[k] = ''; });
        estado.page = 0; cargar();
      }
    }, 'Limpiar'));
  }
  cont.append(barra, tablaWrap, pag);

  async function cargar() {
    tablaWrap.innerHTML = '';
    tablaWrap.appendChild(h('div', { class: 'vacio' }, 'Cargando...'));
    try {
      const data = await api(cfg.endpoint, { query: { ...estado.filtros, ...(cfg.queryFija || {}), page: estado.page, size: estado.size, sort: estado.sort } });
      pintar(data);
    } catch (e) {
      tablaWrap.innerHTML = '';
      tablaWrap.appendChild(alertaError(e));
      pag.innerHTML = '';
    }
  }

  function pintar(data) {
    tablaWrap.innerHTML = '';
    const filas = data.content || [];
    const thead = h('tr', null, cfg.columnas.map(c => {
      if (!c.sort) return h('th', null, c.titulo);
      const [campoSort, dir] = (estado.sort || '').split(',');
      const flecha = campoSort === c.sort ? (dir === 'desc' ? ' ▼' : ' ▲') : '';
      return h('th', {
        class: 'ordenable', onclick: () => {
          estado.sort = c.sort + ',' + (campoSort === c.sort && dir !== 'desc' ? 'desc' : 'asc');
          cargar();
        }
      }, c.titulo + flecha);
    }), cfg.acciones ? h('th', { class: 'right' }, 'Acciones') : null);
    const tbody = h('tbody');
    if (!filas.length) {
      tbody.appendChild(h('tr', null, h('td', { colspan: cfg.columnas.length + 1, class: 'vacio' }, cfg.vacio || 'No hay registros')));
    }
    filas.forEach(f => {
      tbody.appendChild(h('tr', null,
        cfg.columnas.map(c => h('td', null, c.render ? c.render(f) : (f[c.campo] ?? ''))),
        cfg.acciones ? h('td', { class: 'acciones' }, cfg.acciones(f, api_)) : null));
    });
    tablaWrap.appendChild(h('table', null, h('thead', null, thead), tbody));

    pag.innerHTML = '';
    const total = data.totalElements || 0;
    const sizeSel = h('select', { onchange: e => { estado.size = Number(e.target.value); estado.page = 0; cargar(); } },
      [10, 20, 50, 100].map(n => h('option', { value: n, selected: n === estado.size }, n + ' por pagina')));
    pag.append(
      h('span', { class: 'muted small' }, `${total} registro(s) - pagina ${total ? data.page + 1 : 0} de ${data.totalPages || 0}`),
      sizeSel,
      h('button', { class: 'btn btn-sm', disabled: data.first, onclick: () => { estado.page--; cargar(); } }, '‹ Anterior'),
      h('button', { class: 'btn btn-sm', disabled: data.last, onclick: () => { estado.page++; cargar(); } }, 'Siguiente ›'));
  }

  const api_ = { recargar: cargar, estado };
  cargar();
  return api_;
}

/** Bloque "detalle" de pares etiqueta-valor. */
function detalle(pares) {
  return h('div', { class: 'detalle' }, pares.filter(p => p).map(([k, v]) => h('div', null, h('b', null, k), v === null || v === undefined || v === '' ? '—' : v)));
}

function boton(texto, onClick, clase = '') { return h('button', { class: 'btn btn-sm ' + clase, type: 'button', onclick: onClick }, texto); }

/** Ejecuta una accion contra la API mostrando toast y recargando. */
async function accion(promesa, ok, recargar) {
  try { await promesa(); if (ok) toast(ok, 'ok'); if (recargar) recargar(); }
  catch (e) { toast(errorTexto(e), 'error'); }
}

/* ------------------------------------------------------------------ Helpers usados por los modulos de procesos */

function paramsHash() {
  const q = location.hash.split('?')[1] || '';
  return Object.fromEntries(new URLSearchParams(q));
}

let MI_FICHA = null;
async function miFicha() {
  if (!MI_FICHA && esCliente()) MI_FICHA = await api('/clientes/me');
  return MI_FICHA;
}

const columnaPago = f => f.pagoId
  ? h('div', null, badge(f.estadoPago), f.saldo > 0 ? h('div', { class: 'small muted' }, 'Saldo ' + fmt.dinero(f.saldo)) : null)
  : h('span', { class: 'muted small' }, 'Sin cuenta');

function botonCuenta(f) { return f.pagoId && typeof detallePago === 'function' ? boton('Cuenta', () => detallePago(f.pagoId)) : null; }

