/* Catalogos (categorias, modalidades, docentes, espacios, servicios de catering), usuarios, roles y perfil. */

const CATALOGOS = {
  categorias: {
    titulo: 'Categorias', desc: 'Temas de la oferta academica. Desactivar no afecta ofertas existentes.',
    endpoint: '/categorias',
    columnas: [{ titulo: 'Nombre', campo: 'nombre', sort: 'nombre' }, { titulo: 'Descripcion', campo: 'descripcion' }],
    campos: [{ name: 'nombre', label: 'Nombre', required: true, full: true, maxlength: 100 },
      { name: 'descripcion', label: 'Descripcion', type: 'textarea', full: true, maxlength: 250 }]
  },
  modalidades: {
    titulo: 'Modalidades', desc: 'Presencial exige espacio fisico; virtual lo prohibe.',
    endpoint: '/modalidades',
    columnas: [{ titulo: 'Nombre', campo: 'nombre', sort: 'nombre' },
      { titulo: 'Requiere espacio fisico', render: f => fmt.bool(f.requiereEspacioFisico), sort: 'requiereEspacioFisico' }],
    campos: [{ name: 'nombre', label: 'Nombre', required: true, maxlength: 50 },
      { name: 'requiereEspacioFisico', label: 'Requiere espacio fisico (aula, laboratorio...)', type: 'checkbox' }]
  },
  docentes: {
    titulo: 'Docentes', desc: 'No se desactiva un docente con ofertas vigentes.',
    endpoint: '/docentes',
    columnas: [{ titulo: 'Nombre', campo: 'nombre', sort: 'nombre' }, { titulo: 'Especialidad', campo: 'especialidad', sort: 'especialidad' },
      { titulo: 'Correo', campo: 'correo', sort: 'correo' }, { titulo: 'Telefono', campo: 'telefono' }],
    campos: [{ name: 'nombre', label: 'Nombre', required: true, maxlength: 150 },
      { name: 'especialidad', label: 'Especialidad', maxlength: 150 },
      { name: 'correo', label: 'Correo', type: 'email', required: true },
      { name: 'telefono', label: 'Telefono', placeholder: '7123-4567', ayuda: 'Formato ####-####' }]
  }
};

/** CRUD generico de un catalogo con activar/desactivar. Escritura solo ADMIN. */
function vistaCatalogo(cont, clave, extra = {}) {
  const cfg = { ...CATALOGOS[clave], ...extra };
  const crear = esAdmin() ? h('button', { class: 'btn btn-primary', onclick: () => formulario(null) }, '+ Nuevo') : null;
  encabezado(cont, cfg.titulo, cfg.desc, crear);
  const card = h('div', { class: 'card' });
  cont.appendChild(card);

  const lista = listado(card, {
    endpoint: cfg.endpoint,
    sortInicial: cfg.sortInicial || 'nombre,asc',
    filtros: [{ name: 'q', label: 'Buscar', placeholder: 'Texto...' }, ...(cfg.filtrosExtra || []),
      { name: 'activo', label: 'Estado', type: 'select', options: opBool }],
    columnas: [...cfg.columnas, { titulo: 'Estado', render: f => badge(f.activo), sort: 'activo' }],
    acciones: f => esAdmin() ? [
      boton('Editar', () => formulario(f)),
      f.activo
        ? boton('Desactivar', async () => {
          if (await confirmar('¿Desactivar "' + (f.nombre) + '"?', { textoBoton: 'Desactivar', clase: 'btn-danger' }) !== null)
            accion(() => api(cfg.endpoint + '/' + f.id, { method: 'DELETE' }), 'Desactivado', lista.recargar);
        }, 'btn-danger')
        : boton('Activar', () => accion(() => api(cfg.endpoint + '/' + f.id + '/activar', { method: 'PATCH' }), 'Activado', lista.recargar))
    ] : []
  });

  function formulario(fila) {
    modalFormulario({
      titulo: (fila ? 'Editar ' : 'Nuevo ') + cfg.titulo.toLowerCase().replace(/s$/, ''),
      campos: cfg.campos, inicial: fila || {},
      onGuardar: async v => {
        if (cfg.antesDeGuardar) cfg.antesDeGuardar(v);
        await api(cfg.endpoint + (fila ? '/' + fila.id : ''), { method: fila ? 'PUT' : 'POST', body: v });
        toast('Guardado', 'ok');
        lista.recargar();
      }
    });
  }
}

/* ------------------------------------------------------------------ Usuarios y roles (ADMIN) */
function vistaUsuarios(cont) {
  encabezado(cont, 'Usuarios', 'Cuentas del personal. Las cuentas CLIENTE nacen con su ficha (registro publico o Clientes > Crear acceso).',
    h('button', { class: 'btn btn-primary', onclick: () => crear() }, '+ Nuevo usuario'));
  const card = h('div', { class: 'card' });
  cont.appendChild(card);
  const yo = Session.user.id;
  const lista = listado(card, {
    endpoint: '/usuarios', sortInicial: 'id,desc',
    filtros: [{ name: 'q', label: 'Buscar', placeholder: 'Nombre o correo' },
      { name: 'rol', label: 'Rol', type: 'select', options: op(ENUMS.Rol) },
      { name: 'activo', label: 'Estado', type: 'select', options: opBool }],
    columnas: [{ titulo: 'Nombre', campo: 'nombre', sort: 'nombre' }, { titulo: 'Correo', campo: 'email', sort: 'email' },
      { titulo: 'Rol', render: f => fmt.enum(f.rol), sort: 'rol' }, { titulo: 'Ultimo acceso', render: f => fmt.fechaHora(f.ultimoAcceso), sort: 'ultimoAcceso' },
      { titulo: 'Estado', render: f => badge(f.activo), sort: 'activo' }],
    acciones: f => [
      boton('Editar', () => editar(f)),
      f.rol !== 'CLIENTE' && f.id !== yo ? boton('Rol', () => cambiarRol(f)) : null,
      boton('Contrasena', () => password(f)),
      f.id === yo ? null : f.activo
        ? boton('Desactivar', async () => {
          if (await confirmar('¿Desactivar la cuenta de ' + f.nombre + '? No podra iniciar sesion.', { textoBoton: 'Desactivar', clase: 'btn-danger' }) !== null)
            accion(() => api('/usuarios/' + f.id, { method: 'DELETE' }), 'Cuenta desactivada', lista.recargar);
        }, 'btn-danger')
        : boton('Activar', () => accion(() => api('/usuarios/' + f.id + '/activar', { method: 'PATCH' }), 'Cuenta activada', lista.recargar))
    ]
  });

  function crear() {
    modalFormulario({
      titulo: 'Nuevo usuario del personal',
      campos: [{ name: 'nombre', label: 'Nombre', required: true }, { name: 'email', label: 'Correo', type: 'email', required: true },
        { name: 'password', label: 'Contrasena temporal', type: 'password', required: true, ayuda: 'Minimo 8 caracteres con letras y numeros' },
        { name: 'rol', label: 'Rol', type: 'select', required: true, options: op(ENUMS.RolPersonal) }],
      onGuardar: async v => { await api('/usuarios', { method: 'POST', body: v }); toast('Usuario creado', 'ok'); lista.recargar(); }
    });
  }
  function editar(f) {
    modalFormulario({
      titulo: 'Editar usuario', inicial: f,
      campos: [{ name: 'nombre', label: 'Nombre', required: true }, { name: 'email', label: 'Correo', type: 'email', required: true, ayuda: 'Si cambia, se cierran sus sesiones' }],
      onGuardar: async v => { await api('/usuarios/' + f.id, { method: 'PUT', body: v }); toast('Actualizado', 'ok'); lista.recargar(); }
    });
  }
  function cambiarRol(f) {
    modalFormulario({
      titulo: 'Cambiar rol de ' + f.nombre, inicial: f,
      campos: [{ name: 'rol', label: 'Rol', type: 'select', required: true, options: op(ENUMS.RolPersonal), ayuda: 'Sus sesiones abiertas se cerraran' }],
      onGuardar: async v => { await api('/usuarios/' + f.id + '/rol', { method: 'PATCH', body: v }); toast('Rol actualizado', 'ok'); lista.recargar(); }
    });
  }
  function password(f) {
    modalFormulario({
      titulo: 'Contrasena temporal para ' + f.nombre,
      campos: [{ name: 'nuevaPassword', label: 'Nueva contrasena', type: 'password', required: true }],
      onGuardar: async v => { await api('/usuarios/' + f.id + '/password', { method: 'PATCH', body: v }); toast('Contrasena asignada', 'ok'); }
    });
  }
}

async function vistaRoles(cont) {
  encabezado(cont, 'Roles y permisos', 'Lo que puede hacer cada rol (lo aplica el backend con @PreAuthorize).');
  const grid = h('div', { class: 'grid grid-2' });
  cont.appendChild(grid);
  try {
    (await api('/roles')).forEach(r => grid.appendChild(h('div', { class: 'card' },
      h('div', { class: 'row' }, h('h2', null, r.nombre), h('div', { class: 'spacer' }), h('span', { class: 'badge b-info' }, r.usuariosActivos + ' activos')),
      h('p', { class: 'muted' }, r.descripcion), h('ul', { class: 'lista-simple' }, r.permisos.map(p => h('li', null, p))))));
  } catch (e) { cont.appendChild(alertaError(e)); }
}

/* ------------------------------------------------------------------ Perfil */
async function vistaPerfil(cont) {
  encabezado(cont, 'Mi perfil', 'Tu cuenta y tu contrasena.');
  try {
    const u = await api('/auth/me');
    cont.appendChild(h('div', { class: 'card' }, detalle([['Nombre', u.nombre], ['Correo', u.email], ['Rol', fmt.enum(u.rol)],
      ['Ultimo acceso', fmt.fechaHora(u.ultimoAcceso)], ['Cuenta creada', fmt.fechaHora(u.fechaCreacion)]])));
  } catch (e) { cont.appendChild(alertaError(e)); }
  const f = crearFormulario([
    { name: 'passwordActual', label: 'Contrasena actual', type: 'password', required: true },
    { name: 'nuevaPassword', label: 'Nueva contrasena', type: 'password', required: true, ayuda: 'Minimo 8 caracteres con letras y numeros' },
    { name: 'confirmacion', label: 'Repite la nueva contrasena', type: 'password', required: true }
  ]);
  cont.appendChild(h('div', { class: 'card' }, h('h2', null, 'Cambiar contrasena'),
    h('p', { class: 'muted small' }, 'Se cerraran tus sesiones en otros dispositivos.'), f.errorGeneral, f.form,
    h('div', { class: 'row', style: 'margin-top:.8rem' }, h('button', {
      class: 'btn btn-primary', onclick: async () => {
        f.mostrarError(null);
        const v = f.valores();
        if (v.nuevaPassword !== v.confirmacion) { f.mostrarError({ message: 'Las contrasenas no coinciden', details: [] }); return; }
        try {
          Session.save(await api('/auth/password', { method: 'PUT', body: { passwordActual: v.passwordActual, nuevaPassword: v.nuevaPassword } }));
          toast('Contrasena actualizada', 'ok');
          f.form.reset();
        } catch (e) { f.mostrarError(e); }
      }
    }, 'Actualizar contrasena'))));
}
