// ============================================================
// Motor CRUD generico. Toda la logica de "como se ve y como se
// comporta" un modulo vive en config.js; aqui solo se interpreta
// esa configuracion.
// ============================================================

let moduloActual = null;
let registroEnEdicion = null;
const cacheOpciones = {}; // cache de listas para los <select> (categorias, clientes, etc.)

function tieneRol(roles) {
  return roles.includes(getRol());
}

function puedeCrear(m) { return (m.canCreate !== false) && tieneRol(m.writeRoles || []); }
function puedeEditar(m) { return (m.canEdit !== false) && tieneRol(m.writeRoles || []); }
function puedeBorrar(m) { return (m.deleteRoles || []).length > 0 && tieneRol(m.deleteRoles); }

// ---------- Render principal ----------

async function renderModule(key) {
  moduloActual = getModule(key);
  document.getElementById('tituloModulo').textContent = moduloActual.label;

  const content = document.getElementById('contentArea');

  if (moduloActual.esAgenda) return renderAgenda(content);
  if (moduloActual.esPagos) return renderPagos(content);

  content.innerHTML = `
    <div class="card">
      <div class="toolbar">
        <div></div>
        ${puedeCrear(moduloActual) ? `<button class="btn btn-dorado" onclick="abrirFormulario()">+ Nuevo</button>` : ''}
      </div>
      <div id="tablaWrap"><div class="cargando">Cargando...</div></div>
    </div>`;

  await cargarYRenderizarTabla();
}

async function cargarYRenderizarTabla() {
  const wrap = document.getElementById('tablaWrap');
  try {
    const datos = await apiFetch(moduloActual.endpoint);
    wrap.innerHTML = construirTabla(moduloActual, datos);
  } catch (err) {
    wrap.innerHTML = `<div class="vacio">${err.message}</div>`;
  }
}

function construirTabla(m, datos) {
  if (!datos || datos.length === 0) {
    return `<div class="vacio">No hay registros todavía.</div>`;
  }

  const mostrarAcciones = puedeEditar(m) || puedeBorrar(m) || m.estado;

  let html = '<table><thead><tr>';
  m.columns.forEach(c => html += `<th>${c.label}</th>`);
  if (mostrarAcciones) html += '<th></th>';
  html += '</tr></thead><tbody>';

  datos.forEach(fila => {
    html += '<tr>';
    m.columns.forEach(c => html += `<td>${formatearCelda(c, fila[c.key])}</td>`);
    if (mostrarAcciones) {
      html += `<td class="acciones-col">${construirAcciones(m, fila)}</td>`;
    }
    html += '</tr>';
  });

  html += '</tbody></table>';
  return html;
}

function formatearCelda(columna, valor) {
  if (valor === null || valor === undefined || valor === '') return '<span style="color:#aaa">—</span>';
  if (columna.type === 'boolean') {
    return valor
      ? `<span class="badge badge-activo">Sí</span>`
      : `<span class="badge badge-inactivo">No</span>`;
  }
  if (columna.type === 'badge') {
    const clase = 'badge-' + String(valor).toLowerCase();
    return `<span class="badge ${clase}">${valor}</span>`;
  }
  return String(valor);
}

function construirAcciones(m, fila) {
  const id = fila.id;
  let botones = '';

  if (m.estado) {
    botones += `<select class="btn btn-pequeno btn-secundario" style="padding:5px" onchange="cambiarEstado(${id}, this.value)">
        <option value="">Cambiar estado…</option>
        ${m.estado.enum.map(e => `<option value="${e}">${e}</option>`).join('')}
      </select>`;
  }
  if (puedeEditar(m)) {
    botones += `<button class="btn btn-pequeno btn-secundario" onclick='abrirFormulario(${JSON.stringify(fila).replace(/'/g, "&#39;")})'>Editar</button>`;
  }
  if (puedeBorrar(m)) {
    const etiqueta = m.deleteLabel || 'Eliminar';
    botones += `<button class="btn btn-pequeno btn-peligro" onclick="confirmarBorrado(${id}, '${etiqueta}')">${etiqueta}</button>`;
  }
  return botones;
}

// ---------- Formulario (modal) ----------

async function abrirFormulario(registro) {
  registroEnEdicion = registro || null;
  const m = moduloActual;

  document.getElementById('modalTitulo').textContent = registro ? `Editar ${m.label}` : `Nuevo registro: ${m.label}`;
  const body = document.getElementById('modalBody');
  body.innerHTML = '<div class="cargando">Cargando formulario...</div>';
  document.getElementById('modalOverlay').classList.add('visible');

  // Precarga las opciones de todos los <select> que dependen de otro modulo.
  await Promise.all(
    m.fields.filter(f => f.type === 'select' && f.optionsFrom).map(f => cargarOpciones(f.optionsFrom))
  );

  let html = '';
  if (m.formHint) html += `<div class="form-hint" style="margin-bottom:14px">${m.formHint}</div>`;

  m.fields.forEach(f => {
    if (f.hideOnEdit && registro) return; // ej: password no se pide al editar (el backend la ignora ahi)
    const valorActual = registro ? registro[f.name] : '';
    html += `<div class="form-group">
      <label for="campo_${f.name}">${f.label}${f.required ? ' *' : ''}</label>
      ${construirInput(f, valorActual)}
    </div>`;
  });

  body.innerHTML = html;
  document.getElementById('modalGuardarBtn').onclick = guardarFormulario;
}

function construirInput(f, valorActual) {
  const id = `campo_${f.name}`;
  const req = f.required ? 'required' : '';

  if (f.type === 'textarea') {
    return `<textarea id="${id}" rows="3" ${req}>${valorActual || ''}</textarea>`;
  }
  if (f.type === 'boolean') {
    const marcado = valorActual === true ? 'checked' : '';
    return `<select id="${id}"><option value="false" ${!marcado ? 'selected' : ''}>No</option><option value="true" ${marcado ? 'selected' : ''}>Sí</option></select>`;
  }
  if (f.type === 'select') {
    const opciones = f.options ? f.options.map(o => `<option value="${o}" ${o === valorActual ? 'selected' : ''}>${o}</option>`).join('')
      : (cacheOpciones[f.optionsFrom] || []).map(o => `<option value="${o.id}" ${o.id === valorActual ? 'selected' : ''}>${o.nombre || o.titulo}</option>`).join('');
    const vacio = f.optional ? '<option value="">— Ninguno —</option>' : (f.required ? '<option value="">Selecciona…</option>' : '');
    return `<select id="${id}" ${req}>${vacio}${opciones}</select>`;
  }
  const step = f.step ? `step="${f.step}"` : '';
  return `<input type="${f.type}" id="${id}" value="${valorActual !== undefined && valorActual !== null ? valorActual : ''}" ${req} ${step}>`;
}

async function cargarOpciones(moduleKey) {
  if (cacheOpciones[moduleKey]) return;
  const mod = getModule(moduleKey);
  try {
    cacheOpciones[moduleKey] = await apiFetch(mod.endpoint);
  } catch (e) {
    cacheOpciones[moduleKey] = [];
  }
}

async function guardarFormulario() {
  const m = moduloActual;
  const body = {};

  for (const f of m.fields) {
    if (f.hideOnEdit && registroEnEdicion) continue;
    const el = document.getElementById(`campo_${f.name}`);
    let val = el.value;

    if (f.type === 'boolean') val = (val === 'true');
    else if (f.type === 'number') val = val === '' ? null : Number(val);
    else if (f.type === 'select' && !f.options) val = val === '' ? null : Number(val); // FK ids son numericos
    else if (val === '') val = f.optional ? null : val;

    body[f.name] = val;
  }

  const btn = document.getElementById('modalGuardarBtn');
  btn.disabled = true;
  btn.textContent = 'Guardando...';

  try {
    if (registroEnEdicion) {
      await apiFetch(`${m.endpoint}/${registroEnEdicion.id}`, { method: 'PUT', body });
    } else {
      await apiFetch(m.endpoint, { method: 'POST', body });
    }
    cerrarModal();
    await cargarYRenderizarTabla();
  } catch (err) {
    alert('No se pudo guardar: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Guardar';
  }
}

function cerrarModal() {
  document.getElementById('modalOverlay').classList.remove('visible');
  registroEnEdicion = null;
}

// ---------- Borrar / cancelar / desactivar ----------

async function confirmarBorrado(id, etiqueta) {
  if (!confirm(`¿Confirmas: ${etiqueta.toLowerCase()} este registro?`)) return;
  try {
    await apiFetch(`${moduloActual.endpoint}/${id}`, { method: 'DELETE' });
    await cargarYRenderizarTabla();
  } catch (err) {
    alert('No se pudo completar la acción: ' + err.message);
  }
}

// ---------- Cambiar estado (inscripciones, cotizaciones, solicitudes catering) ----------

async function cambiarEstado(id, nuevoEstado) {
  if (!nuevoEstado) return;
  const m = moduloActual;
  try {
    await apiFetch(`${m.estado.endpoint(id)}?estado=${nuevoEstado}`, { method: 'PATCH' });
    await cargarYRenderizarTabla();
  } catch (err) {
    alert('No se pudo cambiar el estado: ' + err.message);
  }
}

// ---------- Agenda (filtro por fechas) ----------

function renderAgenda(content) {
  const hoy = new Date().toISOString().slice(0, 10);
  const en30dias = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);

  content.innerHTML = `
    <div class="card">
      <div class="toolbar filtros">
        <div class="form-group">
          <label>Desde</label>
          <input type="date" id="agendaDesde" value="${hoy}">
        </div>
        <div class="form-group">
          <label>Hasta</label>
          <input type="date" id="agendaHasta" value="${en30dias}">
        </div>
        <button class="btn btn-dorado" onclick="buscarAgenda()">Buscar</button>
      </div>
      <div id="tablaWrap"><div class="cargando">Selecciona un rango y presiona Buscar.</div></div>
    </div>`;
  buscarAgenda();
}

async function buscarAgenda() {
  const desde = document.getElementById('agendaDesde').value;
  const hasta = document.getElementById('agendaHasta').value;
  const wrap = document.getElementById('tablaWrap');
  wrap.innerHTML = '<div class="cargando">Cargando...</div>';
  try {
    const datos = await apiFetch(`/agenda?desde=${desde}&hasta=${hasta}`);
    wrap.innerHTML = construirTabla(moduloActual, datos);
  } catch (err) {
    wrap.innerHTML = `<div class="vacio">${err.message}</div>`;
  }
}

// ---------- Pagos (crear + buscar por referencia/estado + abonar) ----------

function renderPagos(content) {
  content.innerHTML = `
    <div class="card">
      <div class="toolbar">
        <div class="filtros">
          <div class="form-group">
            <label>Buscar por estado</label>
            <select id="pagoFiltroEstado" onchange="buscarPagosPorEstado()">
              <option value="">— Elegir —</option>
              ${ENUMS.EstadoPago.map(e => `<option value="${e}">${e}</option>`).join('')}
            </select>
          </div>
          <div class="form-group">
            <label>Tipo referencia</label>
            <select id="pagoFiltroTipo">
              <option value="">—</option>
              ${ENUMS.TipoReferenciaPago.map(e => `<option value="${e}">${e}</option>`).join('')}
            </select>
          </div>
          <div class="form-group">
            <label>ID referencia</label>
            <input type="number" id="pagoFiltroId" style="width:100px">
          </div>
          <button class="btn btn-secundario" onclick="buscarPagosPorReferencia()">Buscar por referencia</button>
        </div>
        <button class="btn btn-dorado" onclick="abrirFormulario()">+ Nuevo pago</button>
      </div>
      <div id="tablaWrap"><div class="vacio">Busca pagos por estado o por referencia para verlos aquí.</div></div>
    </div>`;
}

async function buscarPagosPorEstado() {
  const estado = document.getElementById('pagoFiltroEstado').value;
  if (!estado) return;
  const wrap = document.getElementById('tablaWrap');
  wrap.innerHTML = '<div class="cargando">Cargando...</div>';
  try {
    const datos = await apiFetch(`/pagos?estado=${estado}`);
    wrap.innerHTML = construirTablaPagos(datos);
  } catch (err) {
    wrap.innerHTML = `<div class="vacio">${err.message}</div>`;
  }
}

async function buscarPagosPorReferencia() {
  const tipo = document.getElementById('pagoFiltroTipo').value;
  const id = document.getElementById('pagoFiltroId').value;
  if (!tipo || !id) { alert('Elige el tipo de referencia y su ID.'); return; }
  const wrap = document.getElementById('tablaWrap');
  wrap.innerHTML = '<div class="cargando">Cargando...</div>';
  try {
    const datos = await apiFetch(`/pagos/referencia?tipo=${tipo}&referenciaId=${id}`);
    wrap.innerHTML = construirTablaPagos(datos);
  } catch (err) {
    wrap.innerHTML = `<div class="vacio">${err.message}</div>`;
  }
}

function construirTablaPagos(datos) {
  if (!datos || datos.length === 0) return `<div class="vacio">No se encontraron pagos.</div>`;
  let html = '<table><thead><tr>';
  moduloActual.columns.forEach(c => html += `<th>${c.label}</th>`);
  html += '<th></th></tr></thead><tbody>';
  datos.forEach(p => {
    html += '<tr>';
    moduloActual.columns.forEach(c => html += `<td>${formatearCelda(c, p[c.key])}</td>`);
    const saldo = (p.monto - p.montoPagado).toFixed(2);
    html += `<td class="acciones-col">`;
    if (p.estado !== 'PAGADO') {
      html += `<button class="btn btn-pequeno btn-dorado" onclick="abonarPago(${p.id}, ${saldo})">Abonar</button>`;
    }
    html += `</td></tr>`;
  });
  html += '</tbody></table>';
  return html;
}

async function abonarPago(id, saldoPendiente) {
  const monto = prompt(`Saldo pendiente: $${saldoPendiente}\n¿Cuánto vas a abonar?`);
  if (!monto) return;
  const montoNum = Number(monto);
  if (isNaN(montoNum) || montoNum <= 0) { alert('Monto inválido.'); return; }
  try {
    await apiFetch(`/pagos/${id}/abonar`, { method: 'PATCH', body: { montoAbonado: montoNum } });
    alert('Abono registrado correctamente.');
    const estado = document.getElementById('pagoFiltroEstado').value;
    if (estado) buscarPagosPorEstado(); else buscarPagosPorReferencia();
  } catch (err) {
    alert('No se pudo abonar: ' + err.message);
  }
}
