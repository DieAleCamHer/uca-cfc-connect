/* Inscripciones (modulo academico). Los botones de estado salen de "accionesPosibles" que calcula el backend. */
/* ------------------------------------------------------------------ Inscripciones */
function vistaInscripciones(cont) {
  const p = paramsHash();
  encabezado(cont, esCliente() ? 'Mis inscripciones' : 'Inscripciones',
    'PENDIENTE -> CONFIRMADA (genera la cuenta) -> FINALIZADA (al terminar y pagar). Cancelar libera el cupo.',
    h('button', { class: 'btn btn-primary', onclick: () => formulario(null, p) }, '+ Nueva inscripcion'));
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const lista = listado(card, {
    endpoint: '/inscripciones', sortInicial: 'id,desc',
    filtros: [esCliente() ? null : { name: 'q', label: 'Buscar', placeholder: 'Participante o cliente' },
      { name: 'estado', label: 'Estado', type: 'select', options: op(ENUMS.EstadoInscripcion) },
      { name: 'cursoId', label: 'Curso', type: 'select', options: async () => (await apiTodos('/cursos', { sort: 'inicio,desc' })).map(c => ({ value: c.id, label: c.nombre })) },
      { name: 'desde', label: 'Inscritas desde', type: 'date' }].filter(Boolean),
    columnas: [
      { titulo: '#', campo: 'id', sort: 'id' },
      { titulo: 'Participante', sort: 'cliente', render: f => h('div', null, h('b', null, f.participanteNombre), f.participanteNombre !== f.clienteNombre ? h('div', { class: 'small muted' }, f.clienteNombre) : null) },
      { titulo: 'Oferta', render: f => h('div', null, f.ofertaNombre, h('div', { class: 'small muted' }, fmt.enum(f.tipoOferta) + ' · ' + fmt.fecha(f.ofertaInicio))) },
      { titulo: 'Fecha', sort: 'fechaInscripcion', render: f => fmt.fecha(f.fechaInscripcion) },
      { titulo: 'Monto', sort: 'monto', render: f => fmt.dinero(f.monto) },
      { titulo: 'Pago', render: columnaPago },
      { titulo: 'Estado', sort: 'estado', render: f => h('div', null, badge(f.estado), f.motivoCancelacion ? h('div', { class: 'small muted' }, f.motivoCancelacion) : null) }
    ],
    acciones: f => [
      f.estado === 'PENDIENTE' && !puede(['CONTABILIDAD']) ? boton('Editar', () => formulario(f)) : null,
      f.accionesPosibles.includes('CONFIRMADA') ? boton('Confirmar', () => accion(() => api(`/inscripciones/${f.id}/confirmar`, { method: 'PATCH' }), 'Inscripcion confirmada: se genero su cuenta por cobrar', lista.recargar), 'btn-ok') : null,
      f.accionesPosibles.includes('FINALIZADA') ? boton('Finalizar', () => accion(() => api(`/inscripciones/${f.id}/finalizar`, { method: 'PATCH' }), 'Finalizada', lista.recargar)) : null,
      f.accionesPosibles.includes('CANCELADA') ? boton('Cancelar', async () => {
        const m = await confirmar('¿Cancelar la inscripcion #' + f.id + '? Se liberara el cupo.', { motivo: true, textoBoton: 'Cancelar inscripcion', clase: 'btn-danger' });
        if (m !== null) accion(() => api('/inscripciones/' + f.id, { method: 'DELETE', query: { motivo: m } }), 'Inscripcion cancelada', lista.recargar);
      }, 'btn-danger') : null,
      botonCuenta(f)
    ]
  });

  async function formulario(fila, pre = {}) {
    const ficha = await miFicha();
    let tipoCliente = ficha ? ficha.tipo : null;
    const vigentes = async rec => (await apiTodos('/' + rec, { sort: 'inicio,asc' }))
      .filter(o => ['PROGRAMADA', 'EN_CURSO'].includes(o.estado))
      .map(o => ({ value: o.id, label: `${o.nombre} · ${fmt.fecha(o.inicio)} · ${fmt.dinero(o.costo)} · ${o.cupoDisponible} cupos`, disabled: o.cupoDisponible < 1 }));
    const inicial = fila ? { clienteId: fila.clienteId, tipoOferta: fila.tipoOferta, cursoId: fila.tipoOferta === 'CURSO' ? fila.ofertaId : null,
      diplomadoId: fila.tipoOferta === 'DIPLOMADO' ? fila.ofertaId : null, participanteNombre: fila.participanteDui ? fila.participanteNombre : null,
      participanteDui: fila.participanteDui, participanteCorreo: fila.participanteCorreo }
      : { clienteId: pre.cliente, tipoOferta: pre.diplomado ? 'DIPLOMADO' : 'CURSO', cursoId: pre.curso, diplomadoId: pre.diplomado };
    if (fila && !esCliente()) tipoCliente = fila.participanteDui ? 'EMPRESA' : 'PERSONA';
    const esEmpresa = v => (esCliente() ? tipoCliente : (CACHE_CLIENTES[v.clienteId] || {}).tipo || tipoCliente) === 'EMPRESA';
    modalFormulario({
      titulo: fila ? 'Editar inscripcion #' + fila.id : 'Nueva inscripcion', inicial,
      campos: [
        esCliente() ? null : { name: 'clienteId', label: 'Cliente', type: 'select', required: true, numerico: true, options: opcionesClientes, full: true, readonly: !!fila },
        { name: 'tipoOferta', label: 'Tipo de oferta', type: 'select', required: true, options: op(['CURSO', 'DIPLOMADO']) },
        { name: 'cursoId', label: 'Curso', type: 'select', numerico: true, options: () => vigentes('cursos'), visible: v => v.tipoOferta === 'CURSO' },
        { name: 'diplomadoId', label: 'Diplomado', type: 'select', numerico: true, options: () => vigentes('diplomados'), visible: v => v.tipoOferta === 'DIPLOMADO' },
        { name: 'participanteNombre', label: 'Nombre del participante', visible: esEmpresa, ayuda: 'La empresa inscribe a un colaborador por inscripcion' },
        { name: 'participanteDui', label: 'DUI del participante', placeholder: '01234567-8', visible: esEmpresa },
        { name: 'participanteCorreo', label: 'Correo del participante', type: 'email', visible: esEmpresa }
      ].filter(Boolean),
      onGuardar: async v => {
        const body = { clienteId: v.clienteId ?? null, cursoId: v.tipoOferta === 'CURSO' ? v.cursoId : null,
          diplomadoId: v.tipoOferta === 'DIPLOMADO' ? v.diplomadoId : null,
          participanteNombre: v.participanteNombre, participanteDui: v.participanteDui, participanteCorreo: v.participanteCorreo };
        await api('/inscripciones' + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body });
        toast(fila ? 'Inscripcion actualizada' : 'Inscripcion registrada (PENDIENTE)', 'ok');
        lista.recargar();
      }
    });
  }
  if (p.curso || p.diplomado || p.cliente) formulario(null, p);
}

