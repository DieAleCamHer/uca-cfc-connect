// Si ya hay sesion activa, no tiene sentido ver el login otra vez.
if (isLoggedIn()) {
  window.location.href = 'app.html';
}

function mostrarTab(cual) {
  const esLogin = cual === 'login';
  document.getElementById('formLogin').style.display = esLogin ? 'block' : 'none';
  document.getElementById('formRegistro').style.display = esLogin ? 'none' : 'block';
  document.getElementById('tabLoginBtn').classList.toggle('activo', esLogin);
  document.getElementById('tabRegistroBtn').classList.toggle('activo', !esLogin);
  ocultarMensajes();
}

function ocultarMensajes() {
  document.getElementById('loginError').classList.remove('visible');
  document.getElementById('loginOk').classList.remove('visible');
}

function mostrarError(msg) {
  const box = document.getElementById('loginError');
  box.textContent = msg;
  box.classList.add('visible');
  document.getElementById('loginOk').classList.remove('visible');
}

function mostrarOk(msg) {
  const box = document.getElementById('loginOk');
  box.textContent = msg;
  box.classList.add('visible');
  document.getElementById('loginError').classList.remove('visible');
}

document.getElementById('formLogin').addEventListener('submit', async (e) => {
  e.preventDefault();
  ocultarMensajes();
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;

  try {
    const data = await apiFetch('/auth/login', { method: 'POST', body: { email, password } });
    sessionStorage.setItem('token', data.token);
    sessionStorage.setItem('rol', data.rol);
    sessionStorage.setItem('nombre', data.nombre);
    sessionStorage.setItem('usuarioId', data.usuarioId);
    window.location.href = 'app.html';
  } catch (err) {
    mostrarError(err.message);
  }
});

document.getElementById('formRegistro').addEventListener('submit', async (e) => {
  e.preventDefault();
  ocultarMensajes();
  const nombre = document.getElementById('regNombre').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value;

  try {
    // El backend ignora cualquier "rol" enviado aqui y siempre crea CLIENTE.
    await apiFetch('/auth/registro', { method: 'POST', body: { nombre, email, password, rol: 'CLIENTE' } });
    mostrarOk('Cuenta creada correctamente. Ya puedes iniciar sesión.');
    document.getElementById('formRegistro').reset();
    setTimeout(() => mostrarTab('login'), 1200);
  } catch (err) {
    mostrarError(err.message);
  }
});
