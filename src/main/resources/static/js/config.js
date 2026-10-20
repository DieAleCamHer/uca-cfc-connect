/*
 * URL del backend.
 * - Si la pagina la sirve el propio backend (http://localhost:8080) se usa el mismo origen.
 * - Si se abre con Live Server (http://127.0.0.1:5500) u otro puerto, se llama a http://localhost:8080
 *   y el navegador aplica CORS (lo permite CorsConfig en el backend).
 * Si el backend esta en otra direccion, cambia BACKEND_REMOTO.
 */
const BACKEND_REMOTO = 'http://localhost:8080';
const BACKEND_URL = window.location.port === '8080' ? window.location.origin : BACKEND_REMOTO;
