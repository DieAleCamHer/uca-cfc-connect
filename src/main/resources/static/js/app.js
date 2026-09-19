// Sin sesion, no hay nada que hacer aqui.
if (!isLoggedIn()) {
  window.location.href = 'index.html';
}

document.getElementById('usuarioNombre').textContent = getNombre() || '—';
document.getElementById('usuarioRol').textContent = traducirRol(getRol());

function traducirRol(rol) {
  const nombres = { ADMIN: 'Administrador', RECEPCIONISTA: 'Recepcionista', CLIENTE: 'Cliente', CONTABILIDAD: 'Contabilidad' };
  return nombres[rol] || rol;
}

construirMenu();

function construirMenu() {
  const rolActual = getRol();
  const nav = document.getElementById('navContainer');
  const modulosVisibles = MODULES.filter(m => m.readRoles.includes(rolActual));

  // Agrupa por el campo "grupo" (Académico, Clientes, etc.), manteniendo el orden de config.js
  const grupos = [];
  modulosVisibles.forEach(m => {
    let g = grupos.find(g => g.nombre === m.grupo);
    if (!g) { g = { nombre: m.grupo, items: [] }; grupos.push(g); }
    g.items.push(m);
  });

  let html = '';
  grupos.forEach(g => {
    html += `<div class="nav-grupo">${g.nombre}</div>`;
    g.items.forEach(m => {
      html += `<a class="nav-item" id="nav_${m.key}" onclick="irAModulo('${m.key}')">${m.label}</a>`;
    });
  });
  nav.innerHTML = html;
}

function irAModulo(key) {
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('activo'));
  document.getElementById(`nav_${key}`).classList.add('activo');
  renderModule(key);
}
