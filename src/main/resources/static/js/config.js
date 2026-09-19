// ============================================================
// ENUMS del backend (deben coincidir EXACTO con los enums de Java)
// ============================================================
const ENUMS = {
  Rol: ['ADMIN', 'RECEPCIONISTA', 'CLIENTE', 'CONTABILIDAD'],
  TipoEspacio: ['AUDITORIO', 'SALA_REUNIONES', 'LABORATORIO', 'AULA', 'SALA_MULTIMEDIA'],
  TipoServicioCatering: ['COFFEE_BREAK', 'DESAYUNO', 'ALMUERZO', 'CENA', 'REFRIGERIO'],
  EstadoSolicitudCatering: ['PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'ATENDIDA'],
  TipoCotizacion: ['CURSO_EMPRESARIAL', 'DIPLOMADO', 'ALQUILER_ESPACIO', 'CATERING', 'SERVICIO_COMBINADO'],
  EstadoCotizacion: ['PENDIENTE', 'EN_PROCESO', 'APROBADA', 'RECHAZADA'],
  EstadoInscripcion: ['PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA'],
  TipoReferenciaPago: ['INSCRIPCION', 'COTIZACION', 'ALQUILER', 'CATERING'],
  MetodoPago: ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'DEPOSITO'],
  EstadoPago: ['PENDIENTE', 'PARCIAL', 'PAGADO'],
  TipoActividad: ['CURSO', 'DIPLOMADO', 'EVENTO', 'ALQUILER', 'CATERING']
};

// Roles con permiso de escritura/lectura por modulo, reflejando EXACTAMENTE
// la matriz de SecurityConfig.java del backend.
const ROLES_ADMIN_RECEP = ['ADMIN', 'RECEPCIONISTA'];
const ROLES_ADMIN_RECEP_CLIENTE = ['ADMIN', 'RECEPCIONISTA', 'CLIENTE'];
const ROLES_TODOS = ['ADMIN', 'RECEPCIONISTA', 'CLIENTE', 'CONTABILIDAD'];

// ============================================================
// MODULOS
// ============================================================
const MODULES = [

  // ---------- Académico ----------
  {
    key: 'categorias', label: 'Categorías', grupo: 'Académico', endpoint: '/categorias',
    readRoles: ROLES_TODOS, writeRoles: ROLES_ADMIN_RECEP, deleteRoles: ['ADMIN'],
    columns: [{ key: 'id', label: 'ID' }, { key: 'nombre', label: 'Nombre' }, { key: 'descripcion', label: 'Descripción' }],
    fields: [
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'descripcion', label: 'Descripción', type: 'textarea' }
    ]
  },
  {
    key: 'modalidades', label: 'Modalidades', grupo: 'Académico', endpoint: '/modalidades',
    readRoles: ROLES_TODOS, writeRoles: ROLES_ADMIN_RECEP, deleteRoles: ['ADMIN'],
    columns: [{ key: 'id', label: 'ID' }, { key: 'nombre', label: 'Nombre' }, { key: 'requiereEspacioFisico', label: 'Requiere espacio', type: 'boolean' }],
    fields: [
      { name: 'nombre', label: 'Nombre (ej. PRESENCIAL, VIRTUAL)', type: 'text', required: true },
      { name: 'requiereEspacioFisico', label: 'Requiere espacio físico', type: 'boolean' }
    ]
  },
  {
    key: 'docentes', label: 'Docentes', grupo: 'Académico', endpoint: '/docentes',
    readRoles: ROLES_TODOS, writeRoles: ROLES_ADMIN_RECEP, deleteRoles: ['ADMIN'],
    columns: [{ key: 'id', label: 'ID' }, { key: 'nombre', label: 'Nombre' }, { key: 'especialidad', label: 'Especialidad' }, { key: 'correo', label: 'Correo' }, { key: 'telefono', label: 'Teléfono' }],
    fields: [
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'especialidad', label: 'Especialidad', type: 'text' },
      { name: 'correo', label: 'Correo', type: 'email' },
      { name: 'telefono', label: 'Teléfono', type: 'text' }
    ]
  },
  {
    key: 'cursos', label: 'Cursos', grupo: 'Académico', endpoint: '/cursos',
    readRoles: ROLES_TODOS, writeRoles: ROLES_ADMIN_RECEP, deleteRoles: ['ADMIN'], deleteLabel: 'Desactivar',
    columns: [
      { key: 'id', label: 'ID' }, { key: 'nombre', label: 'Nombre' }, { key: 'categoriaNombre', label: 'Categoría' },
      { key: 'modalidadNombre', label: 'Modalidad' }, { key: 'docenteNombre', label: 'Docente' }, { key: 'espacioNombre', label: 'Espacio' },
      { key: 'cupoMaximo', label: 'Cupo' }, { key: 'fechaInicio', label: 'Inicio' }, { key: 'fechaFin', label: 'Fin' },
      { key: 'costo', label: 'Costo' }, { key: 'activo', label: 'Activo', type: 'boolean' }
    ],
    fields: [
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'categoriaId', label: 'Categoría', type: 'select', optionsFrom: 'categorias', required: true },
      { name: 'modalidadId', label: 'Modalidad', type: 'select', optionsFrom: 'modalidades', required: true },
      { name: 'docenteId', label: 'Docente', type: 'select', optionsFrom: 'docentes', required: true },
      { name: 'espacioId', label: 'Espacio (solo si la modalidad requiere uno físico)', type: 'select', optionsFrom: 'espacios', optional: true },
      { name: 'cupoMaximo', label: 'Cupo máximo', type: 'number', required: true },
      { name: 'fechaInicio', label: 'Fecha inicio', type: 'date', required: true },
      { name: 'fechaFin', label: 'Fecha fin', type: 'date', required: true },
      { name: 'horaInicio', label: 'Hora inicio', type: 'time', required: true },
      { name: 'horaFin', label: 'Hora fin', type: 'time', required: true },
      { name: 'costo', label: 'Costo ($)', type: 'number', step: '0.01', required: true }
    ],
    formHint: 'El Espacio solo aplica si la Modalidad elegida requiere espacio físico (ej. PRESENCIAL). Si la modalidad es virtual, déjalo vacío.'
  },
  {
    key: 'diplomados', label: 'Diplomados', grupo: 'Académico', endpoint: '/diplomados',
    readRoles: ROLES_TODOS, writeRoles: ROLES_ADMIN_RECEP, deleteRoles: ['ADMIN'], deleteLabel: 'Desactivar',
    columns: [
      { key: 'id', label: 'ID' }, { key: 'nombre', label: 'Nombre' }, { key: 'categoriaNombre', label: 'Categoría' },
      { key: 'modalidadNombre', label: 'Modalidad' }, { key: 'docenteNombre', label: 'Docente' }, { key: 'espacioNombre', label: 'Espacio' },
      { key: 'cupoMaximo', label: 'Cupo' }, { key: 'fechaInicio', label: 'Inicio' }, { key: 'fechaFin', label: 'Fin' },
      { key: 'costo', label: 'Costo' }, { key: 'activo', label: 'Activo', type: 'boolean' }
    ],
    fields: [
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'categoriaId', label: 'Categoría', type: 'select', optionsFrom: 'categorias', required: true },
      { name: 'modalidadId', label: 'Modalidad', type: 'select', optionsFrom: 'modalidades', required: true },
      { name: 'docenteId', label: 'Docente', type: 'select', optionsFrom: 'docentes', required: true },
      { name: 'espacioId', label: 'Espacio (solo si la modalidad requiere uno físico)', type: 'select', optionsFrom: 'espacios', optional: true },
      { name: 'cupoMaximo', label: 'Cupo máximo', type: 'number', required: true },
      { name: 'fechaInicio', label: 'Fecha inicio', type: 'date', required: true },
      { name: 'fechaFin', label: 'Fecha fin', type: 'date', required: true },
      { name: 'horaInicio', label: 'Hora inicio', type: 'time', required: true },
      { name: 'horaFin', label: 'Hora fin', type: 'time', required: true },
      { name: 'costo', label: 'Costo ($)', type: 'number', step: '0.01', required: true }
    ],
    formHint: 'El Espacio solo aplica si la Modalidad elegida requiere espacio físico (ej. PRESENCIAL). Si la modalidad es virtual, déjalo vacío.'
  },

  // ---------- Clientes ----------
  {
    key: 'clientes', label: 'Clientes', grupo: 'Clientes', endpoint: '/clientes',
    readRoles: ROLES_ADMIN_RECEP_CLIENTE, writeRoles: ROLES_ADMIN_RECEP_CLIENTE, deleteRoles: ROLES_ADMIN_RECEP_CLIENTE,
    columns: [{ key: 'id', label: 'ID' }, { key: 'duiNit', label: 'DUI/NIT' }, { key: 'nombre', label: 'Nombre' }, { key: 'empresa', label: 'Empresa' }, { key: 'correo', label: 'Correo' }, { key: 'telefono', label: 'Teléfono' }],
    fields: [
      { name: 'duiNit', label: 'DUI/NIT', type: 'text', required: true },
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'empresa', label: 'Empresa', type: 'text' },
      { name: 'correo', label: 'Correo', type: 'email', required: true },
      { name: 'telefono', label: 'Teléfono', type: 'text' },
      { name: 'direccion', label: 'Dirección', type: 'textarea' }
    ]
  },

  // ---------- Inscripciones ----------
  {
    key: 'inscripciones', label: 'Inscripciones', grupo: 'Inscripciones', endpoint: '/inscripciones',
    readRoles: ROLES_ADMIN_RECEP_CLIENTE, writeRoles: ROLES_ADMIN_RECEP_CLIENTE, deleteRoles: [],
    canEdit: false,
    columns: [
      { key: 'id', label: 'ID' }, { key: 'clienteNombre', label: 'Cliente' }, { key: 'cursoNombre', label: 'Curso' },
      { key: 'diplomadoNombre', label: 'Diplomado' }, { key: 'fecha', label: 'Fecha' }, { key: 'estado', label: 'Estado', type: 'badge' }
    ],
    fields: [
      { name: 'clienteId', label: 'Cliente', type: 'select', optionsFrom: 'clientes', required: true },
      { name: 'cursoId', label: 'Curso (dejar vacío si es diplomado)', type: 'select', optionsFrom: 'cursos', optional: true },
      { name: 'diplomadoId', label: 'Diplomado (dejar vacío si es curso)', type: 'select', optionsFrom: 'diplomados', optional: true }
    ],
    formHint: 'Debes elegir Curso O Diplomado, nunca ambos ni ninguno.',
    estado: { field: 'estado', enum: ENUMS.EstadoInscripcion, endpoint: (id) => `/inscripciones/${id}/estado` }
  },

  // ---------- Cotizaciones ----------
  {
    key: 'cotizaciones', label: 'Cotizaciones', grupo: 'Cotizaciones', endpoint: '/cotizaciones',
    readRoles: ROLES_ADMIN_RECEP_CLIENTE, writeRoles: ROLES_ADMIN_RECEP_CLIENTE, deleteRoles: [],
    canEdit: false,
    columns: [
      { key: 'id', label: 'ID' }, { key: 'clienteNombre', label: 'Cliente' }, { key: 'tipo', label: 'Tipo' },
      { key: 'total', label: 'Total' }, { key: 'estado', label: 'Estado', type: 'badge' }, { key: 'fechaSolicitud', label: 'Fecha' }
    ],
    fields: [
      { name: 'clienteId', label: 'Cliente', type: 'select', optionsFrom: 'clientes', required: true },
      { name: 'tipo', label: 'Tipo', type: 'select', options: ENUMS.TipoCotizacion, required: true },
      { name: 'descripcion', label: 'Descripción', type: 'textarea' },
      { name: 'total', label: 'Total ($)', type: 'number', step: '0.01', required: true }
    ],
    estado: { field: 'estado', enum: ENUMS.EstadoCotizacion, endpoint: (id) => `/cotizaciones/${id}/estado` }
  },

  // ---------- Alquiler de espacios ----------
  {
    key: 'espacios', label: 'Espacios', grupo: 'Alquiler de espacios', endpoint: '/espacios',
    readRoles: ROLES_TODOS, writeRoles: ROLES_ADMIN_RECEP, deleteRoles: ['ADMIN'], deleteLabel: 'Desactivar',
    columns: [
      { key: 'id', label: 'ID' }, { key: 'nombre', label: 'Nombre' }, { key: 'tipo', label: 'Tipo' },
      { key: 'capacidad', label: 'Capacidad' }, { key: 'precio', label: 'Precio' }, { key: 'disponible', label: 'Disponible', type: 'boolean' }
    ],
    fields: [
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'tipo', label: 'Tipo', type: 'select', options: ENUMS.TipoEspacio, required: true },
      { name: 'capacidad', label: 'Capacidad', type: 'number', required: true },
      { name: 'precio', label: 'Precio ($)', type: 'number', step: '0.01', required: true },
      { name: 'equipamiento', label: 'Equipamiento', type: 'textarea' }
    ]
  },
  {
    key: 'reservas-espacio', label: 'Reservas de espacio', grupo: 'Alquiler de espacios', endpoint: '/reservas-espacio',
    readRoles: ROLES_ADMIN_RECEP_CLIENTE, writeRoles: ROLES_ADMIN_RECEP_CLIENTE, deleteRoles: ROLES_ADMIN_RECEP_CLIENTE,
    canEdit: false, deleteLabel: 'Cancelar',
    columns: [
      { key: 'id', label: 'ID' }, { key: 'espacioNombre', label: 'Espacio' }, { key: 'clienteNombre', label: 'Cliente' },
      { key: 'fechaHoraInicio', label: 'Inicio' }, { key: 'fechaHoraFin', label: 'Fin' }, { key: 'estado', label: 'Estado', type: 'badge' }
    ],
    fields: [
      { name: 'espacioId', label: 'Espacio', type: 'select', optionsFrom: 'espacios', required: true },
      { name: 'clienteId', label: 'Cliente', type: 'select', optionsFrom: 'clientes', required: true },
      { name: 'fechaHoraInicio', label: 'Fecha/hora inicio', type: 'datetime-local', required: true },
      { name: 'fechaHoraFin', label: 'Fecha/hora fin', type: 'datetime-local', required: true },
      { name: 'motivo', label: 'Motivo', type: 'text' }
    ],
    formHint: 'Si el horario se cruza con otra reserva del mismo espacio, el sistema la rechazará automáticamente.'
  },

  // ---------- Catering ----------
  {
    key: 'servicios-catering', label: 'Servicios de catering', grupo: 'Catering', endpoint: '/servicios-catering',
    readRoles: ROLES_TODOS, writeRoles: ROLES_ADMIN_RECEP, deleteRoles: ['ADMIN'],
    columns: [{ key: 'id', label: 'ID' }, { key: 'tipo', label: 'Tipo' }, { key: 'nombre', label: 'Nombre' }, { key: 'precioUnitario', label: 'Precio unitario' }],
    fields: [
      { name: 'tipo', label: 'Tipo', type: 'select', options: ENUMS.TipoServicioCatering, required: true },
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'precioUnitario', label: 'Precio unitario ($)', type: 'number', step: '0.01', required: true }
    ]
  },
  {
    key: 'solicitudes-catering', label: 'Solicitudes de catering', grupo: 'Catering', endpoint: '/solicitudes-catering',
    readRoles: ROLES_ADMIN_RECEP_CLIENTE, writeRoles: ROLES_ADMIN_RECEP_CLIENTE, deleteRoles: [],
    canEdit: false,
    columns: [
      { key: 'id', label: 'ID' }, { key: 'clienteNombre', label: 'Cliente' }, { key: 'servicioNombre', label: 'Servicio' },
      { key: 'numeroAsistentes', label: 'Asistentes' }, { key: 'fecha', label: 'Fecha' }, { key: 'hora', label: 'Hora' },
      { key: 'lugar', label: 'Lugar' }, { key: 'estado', label: 'Estado', type: 'badge' }
    ],
    fields: [
      { name: 'clienteId', label: 'Cliente', type: 'select', optionsFrom: 'clientes', required: true },
      { name: 'servicioId', label: 'Servicio', type: 'select', optionsFrom: 'servicios-catering', required: true },
      { name: 'numeroAsistentes', label: 'Número de asistentes', type: 'number', required: true },
      { name: 'menu', label: 'Menú', type: 'textarea' },
      { name: 'fecha', label: 'Fecha', type: 'date', required: true },
      { name: 'hora', label: 'Hora', type: 'time', required: true },
      { name: 'lugar', label: 'Lugar', type: 'text', required: true }
    ],
    estado: { field: 'estado', enum: ENUMS.EstadoSolicitudCatering, endpoint: (id) => `/solicitudes-catering/${id}/estado` }
  },

  // ---------- Agenda (solo lectura) ----------
  {
    key: 'agenda', label: 'Agenda institucional', grupo: 'Agenda', endpoint: '/agenda',
    readRoles: ROLES_TODOS, writeRoles: [], deleteRoles: [], canCreate: false, canEdit: false,
    esAgenda: true,
    columns: [
      { key: 'id', label: 'ID' }, { key: 'tipo', label: 'Tipo' }, { key: 'titulo', label: 'Título' },
      { key: 'fechaHoraInicio', label: 'Inicio' }, { key: 'fechaHoraFin', label: 'Fin' }, { key: 'espacioNombre', label: 'Espacio' }
    ]
  },

  // ---------- Pagos ----------
  {
    key: 'pagos', label: 'Pagos', grupo: 'Pagos', endpoint: '/pagos',
    readRoles: ['ADMIN', 'CONTABILIDAD'], writeRoles: ['ADMIN', 'CONTABILIDAD'], deleteRoles: [],
    canEdit: false, esPagos: true,
    columns: [
      { key: 'id', label: 'ID' }, { key: 'tipoReferencia', label: 'Referencia' }, { key: 'referenciaId', label: 'ID referencia' },
      { key: 'monto', label: 'Monto' }, { key: 'montoPagado', label: 'Pagado' }, { key: 'metodo', label: 'Método' },
      { key: 'estado', label: 'Estado', type: 'badge' }
    ],
    fields: [
      { name: 'tipoReferencia', label: 'Tipo de referencia', type: 'select', options: ENUMS.TipoReferenciaPago, required: true },
      { name: 'referenciaId', label: 'ID de la referencia (ej. id de la inscripción)', type: 'number', required: true },
      { name: 'monto', label: 'Monto total ($)', type: 'number', step: '0.01', required: true },
      { name: 'metodo', label: 'Método de pago', type: 'select', options: ENUMS.MetodoPago, required: true }
    ]
  },

  // ---------- Seguridad ----------
  {
    key: 'usuarios', label: 'Usuarios', grupo: 'Seguridad', endpoint: '/usuarios',
    readRoles: ['ADMIN'], writeRoles: ['ADMIN'], deleteRoles: ['ADMIN'], deleteLabel: 'Desactivar',
    columns: [{ key: 'id', label: 'ID' }, { key: 'nombre', label: 'Nombre' }, { key: 'email', label: 'Email' }, { key: 'rol', label: 'Rol' }, { key: 'activo', label: 'Activo', type: 'boolean' }],
    fields: [
      { name: 'nombre', label: 'Nombre', type: 'text', required: true },
      { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'password', label: 'Contraseña', type: 'password', required: true, hideOnEdit: true },
      { name: 'rol', label: 'Rol', type: 'select', options: ENUMS.Rol, required: true }
    ]
  }
];

function getModule(key) {
  return MODULES.find(m => m.key === key);
}
