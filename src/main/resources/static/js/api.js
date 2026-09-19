// Todas las llamadas a la API pasan por aqui. Centralizar esto evita repetir
// "agregar el header Authorization" y "manejar errores" en cada modulo.

const API_BASE = window.location.origin + '/api';

function getToken() { return sessionStorage.getItem('token'); }
function getRol() { return sessionStorage.getItem('rol'); }
function getNombre() { return sessionStorage.getItem('nombre'); }
function isLoggedIn() { return !!getToken(); }

function logout() {
  sessionStorage.clear();
  window.location.href = 'index.html';
}

/**
 * path: ej "/clientes" o "/clientes/5"
 * options: { method, body } - body ya en objeto JS (se serializa aqui)
 */
async function apiFetch(path, options = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const fetchOptions = { method: options.method || 'GET', headers };
  if (options.body !== undefined) fetchOptions.body = JSON.stringify(options.body);

  const res = await fetch(API_BASE + path, fetchOptions);

  // Token invalido/expirado: manda de vuelta al login.
  if (res.status === 401 && path !== '/auth/login') {
    logout();
    throw new Error('Tu sesión expiró. Inicia sesión de nuevo.');
  }

  const texto = await res.text();
  let data = null;
  if (texto) {
    try { data = JSON.parse(texto); } catch (e) { data = texto; }
  }

  if (!res.ok) {
    let mensaje = (data && data.message) ? data.message : ('Error ' + res.status);
    if (data && data.detalles && data.detalles.length) {
      mensaje += ' (' + data.detalles.join(', ') + ')';
    }
    throw new Error(mensaje);
  }

  return data;
}
