/* Shell de la aplicacion: menu por rol y enrutamiento por hash (#/cursos, #/pagos...). */
if (!Session.isLogged()) window.location.href = 'login.html';

const ROL = Session.rol;
const R = {
  TODOS: ['ADMIN', 'RECEPCIONISTA', 'CLIENTE', 'CONTABILIDAD'],
  STAFF: ['ADMIN', 'RECEPCIONISTA'],
  STAFF_CLIENTE: ['ADMIN', 'RECEPCIONISTA', 'CLIENTE'],
  CAJA_CLIENTE: ['ADMIN', 'RECEPCIONISTA', 'CONTABILIDAD', 'CLIENTE'],
  FINANZAS: ['ADMIN', 'CONTABILIDAD'],
  ADMIN: ['ADMIN'],
  CLIENTE: ['CLIENTE']
};
const puede = roles => roles.includes(ROL);
const esCliente = () => ROL === 'CLIENTE';
const esStaff = () => puede(R.STAFF);
const esAdmin = () => ROL === 'ADMIN';

/** El menu muestra solo lo que el rol puede usar. La seguridad REAL la aplica el backend (401/403). */
const RUTAS = [
  { grupo: null, ruta: 'inicio', titulo: 'Inicio', roles: R.TODOS, vista: vistaInicio },
  { grupo: 'Oferta academica', ruta: 'cursos', titulo: 'Cursos', roles: R.TODOS, vista: c => vistaOferta(c, 'cursos') },
  { grupo: 'Oferta academica', ruta: 'diplomados', titulo: 'Diplomados', roles: R.TODOS, vista: c => vistaOferta(c, 'diplomados') },
  { grupo: 'Clientes', ruta: 'clientes', titulo: 'Clientes', roles: R.STAFF, vista: vistaClientes },
  { grupo: 'Clientes', ruta: 'mis-datos', titulo: 'Mis datos', roles: R.CLIENTE, vista: vistaMisDatos },
  { grupo: 'Clientes', ruta: 'inscripciones', titulo: () => esCliente() ? 'Mis inscripciones' : 'Inscripciones', roles: R.STAFF_CLIENTE, vista: vistaInscripciones },
  { grupo: 'Alquiler de espacios', ruta: 'espacios', titulo: 'Espacios', roles: R.TODOS, vista: vistaEspacios },
  { grupo: 'Alquiler de espacios', ruta: 'disponibilidad', titulo: 'Disponibilidad', roles: R.STAFF_CLIENTE, vista: vistaDisponibilidad },
  { grupo: 'Alquiler de espacios', ruta: 'reservas', titulo: () => esCliente() ? 'Mis reservas' : 'Reservas', roles: R.STAFF_CLIENTE, vista: vistaReservas },
  { grupo: 'Catering', ruta: 'servicios-catering', titulo: 'Servicios', roles: R.TODOS, vista: vistaServiciosCatering },
  { grupo: 'Catering', ruta: 'catering', titulo: () => esCliente() ? 'Mis pedidos' : 'Solicitudes', roles: R.STAFF_CLIENTE, vista: vistaCatering },
  { grupo: 'Cotizaciones', ruta: 'cotizaciones', titulo: () => esCliente() ? 'Mis cotizaciones' : 'Cotizaciones', roles: R.STAFF_CLIENTE, vista: vistaCotizaciones },
  { grupo: 'Agenda', ruta: 'agenda', titulo: 'Calendario', roles: R.STAFF_CLIENTE, vista: vistaAgenda },
  { grupo: 'Agenda', ruta: 'eventos', titulo: 'Eventos', roles: R.STAFF, vista: vistaEventos },
  { grupo: 'Pagos', ruta: 'pagos', titulo: () => esCliente() ? 'Mis pagos' : 'Cuentas por cobrar', roles: R.CAJA_CLIENTE, vista: vistaPagos },
  { grupo: 'Pagos', ruta: 'abonos', titulo: () => puede(R.FINANZAS) ? 'Validar pagos' : 'Abonos', roles: R.CAJA_CLIENTE, vista: vistaAbonos },
  { grupo: 'Reportes', ruta: 'reportes', titulo: 'Reportes', roles: R.FINANZAS, vista: vistaReportes },
  { grupo: 'Configuracion', ruta: 'categorias', titulo: 'Categorias', roles: R.STAFF, vista: c => vistaCatalogo(c, 'categorias') },
  { grupo: 'Configuracion', ruta: 'modalidades', titulo: 'Modalidades', roles: R.STAFF, vista: c => vistaCatalogo(c, 'modalidades') },
  { grupo: 'Configuracion', ruta: 'docentes', titulo: 'Docentes', roles: R.STAFF, vista: c => vistaCatalogo(c, 'docentes') },
  { grupo: 'Seguridad', ruta: 'usuarios', titulo: 'Usuarios', roles: R.ADMIN, vista: vistaUsuarios },
  { grupo: 'Seguridad', ruta: 'roles', titulo: 'Roles y permisos', roles: R.ADMIN, vista: vistaRoles },
  { grupo: null, ruta: 'perfil', titulo: 'Mi perfil', roles: R.TODOS, vista: vistaPerfil, oculto: true }
];
const titulo = r => typeof r.titulo === 'function' ? r.titulo() : r.titulo;

function construirMenu() {
  const nav = document.getElementById('menu');
  nav.innerHTML = '';
  let grupoActual;
  RUTAS.filter(r => puede(r.roles) && !r.oculto).forEach(r => {
    if (r.grupo !== grupoActual) {
      grupoActual = r.grupo;
      if (r.grupo) nav.appendChild(h('div', { class: 'nav-group' }, r.grupo));
    }
    nav.appendChild(h('a', { class: 'nav-item', href: '#/' + r.ruta, 'data-ruta': r.ruta }, titulo(r)));
  });
}

function navegar() {
  const ruta = (location.hash.replace(/^#\/?/, '').split('?')[0]) || 'inicio';
  const r = RUTAS.find(x => x.ruta === ruta && puede(x.roles)) || RUTAS[0];
  document.querySelectorAll('.nav-item').forEach(a => a.classList.toggle('activo', a.dataset.ruta === r.ruta));
  const raiz = document.getElementById('contenido');
  raiz.innerHTML = '';
  // Contenedor nuevo por navegacion: si una vista anterior termina de cargar tarde, escribe en un nodo ya desconectado.
  const cont = h('div');
  raiz.appendChild(cont);
  document.title = titulo(r) + ' - UCA-CFC Connect';
  document.getElementById('sidebar').classList.remove('abierto');
  try {
    r.vista(cont);
  } catch (e) {
    cont.appendChild(alertaError(e));
    console.error(e);
  }
}

function irA(ruta) { location.hash = '#/' + ruta; }

/** Encabezado comun de cada pantalla. */
function encabezado(cont, tituloTxt, descripcion, ...botones) {
  cont.appendChild(h('div', { class: 'page-head' },
    h('div', null, h('h1', null, tituloTxt), descripcion ? h('p', null, descripcion) : null),
    h('div', { class: 'spacer' }), botones));
}

(function iniciar() {
  const u = Session.user;
  document.getElementById('usuarioNombre').textContent = u.nombre;
  document.getElementById('usuarioRol').textContent = fmt.enum(u.rol);
  document.getElementById('btnMenu').onclick = () => document.getElementById('sidebar').classList.toggle('abierto');
  document.getElementById('btnPerfil').onclick = () => irA('perfil');
  document.getElementById('btnSalir').onclick = async () => {
    try { await api('/auth/logout', { method: 'POST' }); } catch (e) { /* aunque falle, se cierra localmente */ }
    Session.clear();
    window.location.href = 'index.html';
  };
  construirMenu();
  window.addEventListener('hashchange', navegar);
  navegar();
})();
