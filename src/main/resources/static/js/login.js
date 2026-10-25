/* Login, registro de clientes y recuperacion de contrasena. */
(function () {
  const params = new URLSearchParams(location.search);
  const msg = document.getElementById('mensaje');
  if (Session.isLogged() && !params.get('motivo')) { location.href = 'app.html'; return; }
  if (params.get('motivo')) mostrar(params.get('motivo'), 'info');

  function mostrar(texto, tipo = 'error', detalles) {
    msg.innerHTML = '';
    msg.appendChild(h('div', { class: 'alerta alerta-' + tipo }, texto, detalles && detalles.length ? h('ul', null, detalles.map(d => h('li', null, d))) : null));
  }
  const paneles = { login: 'formLogin', registro: 'formRegistro', recuperar: 'panelRecuperar' };
  function abrir(tab) {
    document.querySelectorAll('.tab').forEach(t => t.classList.toggle('activo', t.dataset.tab === tab));
    Object.entries(paneles).forEach(([k, id]) => document.getElementById(id).classList.toggle('hidden', k !== tab));
  }
  document.querySelectorAll('.tab').forEach(t => t.addEventListener('click', () => { msg.innerHTML = ''; abrir(t.dataset.tab); }));
  if (params.get('tab')) abrir(params.get('tab'));

  const datos = form => Object.fromEntries([...new FormData(form).entries()].map(([k, v]) => [k, typeof v === 'string' && v.trim() === '' ? null : v]));

  document.getElementById('formLogin').addEventListener('submit', async e => {
    e.preventDefault();
    try {
      Session.save(await api('/auth/login', { method: 'POST', body: datos(e.target), auth: false }));
      location.href = 'app.html';
    } catch (err) { mostrar(err.message, 'error', err.details); }
  });

  const tipo = document.querySelector('#formRegistro [name=tipo]');
  tipo.addEventListener('change', () => { document.getElementById('campoEmpresa').style.display = tipo.value === 'EMPRESA' ? '' : 'none'; });
  document.getElementById('formRegistro').addEventListener('submit', async e => {
    e.preventDefault();
    const body = datos(e.target);
    if (body.tipo !== 'EMPRESA') body.empresa = null;
    try {
      Session.save(await api('/auth/registro', { method: 'POST', body, auth: false }));
      location.href = 'app.html';
    } catch (err) { mostrar(err.message, 'error', err.details); }
  });

  document.getElementById('formRecuperar').addEventListener('submit', async e => {
    e.preventDefault();
    try {
      const r = await api('/auth/recuperar', { method: 'POST', body: datos(e.target), auth: false });
      if (r.tokenDemo) {
        document.querySelector('#formRestablecer [name=token]').value = r.tokenDemo;
        mostrar(r.mensaje + ' (Modo academico: no hay servidor de correo, el codigo ya se copio abajo.)', 'ok');
      } else mostrar(r.mensaje, 'ok');
    } catch (err) { mostrar(err.message, 'error', err.details); }
  });
  document.getElementById('formRestablecer').addEventListener('submit', async e => {
    e.preventDefault();
    try {
      const r = await api('/auth/restablecer', { method: 'POST', body: datos(e.target), auth: false });
      mostrar(r.mensaje, 'ok');
      abrir('login');
    } catch (err) { mostrar(err.message, 'error', err.details); }
  });
})();
