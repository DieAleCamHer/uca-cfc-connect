/* Cliente HTTP unico de la aplicacion. BACKEND_URL viene de config.js (mismo origen o CORS). */
const API_BASE = BACKEND_URL + '/api';
const KEYS = { access: 'cfc.access', refresh: 'cfc.refresh', user: 'cfc.user' };

const Session = {
  get access() { return localStorage.getItem(KEYS.access); },
  get refresh() { return localStorage.getItem(KEYS.refresh); },
  get user() { try { return JSON.parse(localStorage.getItem(KEYS.user)); } catch (e) { return null; } },
  get rol() { return this.user ? this.user.rol : null; },
  save(auth) {
    localStorage.setItem(KEYS.access, auth.accessToken);
    if (auth.refreshToken) localStorage.setItem(KEYS.refresh, auth.refreshToken);
    if (auth.usuario) localStorage.setItem(KEYS.user, JSON.stringify(auth.usuario));
  },
  saveUser(u) { localStorage.setItem(KEYS.user, JSON.stringify(u)); },
  clear() { Object.values(KEYS).forEach(k => localStorage.removeItem(k)); },
  isLogged() { return !!this.access && !!this.user; }
};

class ApiError extends Error {
  constructor(status, code, message, details) {
    super(message || ('Error ' + status));
    this.status = status; this.code = code; this.details = details || [];
  }
  get texto() { return this.details.length ? this.message + ': ' + this.details.join('; ') : this.message; }
}

function buildUrl(path, query) {
  const url = new URL(API_BASE + path);
  if (query) {
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
    });
  }
  return url.toString();
}

let renovando = null;
async function renovarToken() {
  if (!Session.refresh) return false;
  if (!renovando) {
    renovando = fetch(buildUrl('/auth/refresh'), {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: Session.refresh })
    }).then(async r => {
      if (!r.ok) return false;
      Session.save(await r.json());
      return true;
    }).catch(() => false).finally(() => { setTimeout(() => { renovando = null; }, 0); });
  }
  return renovando;
}

function expulsar(motivo) {
  Session.clear();
  window.location.href = 'login.html' + (motivo ? '?motivo=' + encodeURIComponent(motivo) : '');
}

/**
 * api('/cursos', { query: {page:0}, method: 'POST', body: {...}, auth: true })
 * - Agrega Authorization: Bearer.
 * - Si el access token vencio (401 TOKEN_EXPIRED) lo renueva con el refresh token y reintenta UNA vez.
 * - Cualquier otro 401 (revocado, cuenta desactivada) cierra la sesion.
 * - Errores -> ApiError con status, code, message y details del ErrorResponse del backend.
 */
async function api(path, opts = {}) {
  const { method = 'GET', body, query, auth = true, _reintento = false } = opts;
  const headers = { 'Accept': 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth && Session.access) headers['Authorization'] = 'Bearer ' + Session.access;

  let res;
  try {
    res = await fetch(buildUrl(path, query), { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch (e) {
    throw new ApiError(0, 'NETWORK', 'No se pudo conectar con el servidor (' + BACKEND_URL + '). Verifica que el backend este corriendo.');
  }

  const texto = await res.text();
  let data = null;
  if (texto) { try { data = JSON.parse(texto); } catch (e) { data = { message: texto }; } }

  if (res.status === 401 && auth) {
    const code = data && data.code;
    if (code === 'TOKEN_EXPIRED' && !_reintento && await renovarToken()) {
      return api(path, { ...opts, _reintento: true });
    }
    if (code !== 'BAD_CREDENTIALS') {
      expulsar((data && data.message) || 'Tu sesion termino');
      throw new ApiError(401, code, (data && data.message) || 'Sesion terminada');
    }
  }
  if (!res.ok) {
    throw new ApiError(res.status, data && data.code, (data && data.message) || ('Error ' + res.status), data && data.details);
  }
  return data;
}

/** Trae hasta 100 registros de un listado paginado (para llenar selects). */
async function apiTodos(path, query = {}) {
  const page = await api(path, { query: { size: 100, ...query } });
  return page && page.content ? page.content : (page || []);
}
