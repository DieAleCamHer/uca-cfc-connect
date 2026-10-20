/* Pagina publica: oferta vigente sin necesidad de token (/api/publico/**). */
(function () {
  document.getElementById('lnkSwagger').href = BACKEND_URL + '/swagger-ui.html';
  if (Session.isLogged()) {
    const b = document.getElementById('btnEntrar');
    b.textContent = 'Ir a mi panel'; b.href = 'app.html';
  }
  const res = document.getElementById('resultado');
  const q = document.getElementById('q'), cat = document.getElementById('categoria'), mod = document.getElementById('modalidad');
  let tab = 'cursos';

  api('/publico/categorias', { auth: false }).then(p => p.content.forEach(c => cat.appendChild(h('option', { value: c.id }, c.nombre)))).catch(() => { });
  api('/publico/modalidades', { auth: false }).then(p => p.content.forEach(c => mod.appendChild(h('option', { value: c.id }, c.nombre)))).catch(() => { });

  document.querySelectorAll('#tabs .tab').forEach(b => b.addEventListener('click', () => {
    document.querySelectorAll('#tabs .tab').forEach(x => x.classList.remove('activo'));
    b.classList.add('activo'); tab = b.dataset.tab;
    document.getElementById('filtros').classList.toggle('hidden', !['cursos', 'diplomados'].includes(tab));
    cargar();
  }));
  let t;
  q.addEventListener('input', () => { clearTimeout(t); t = setTimeout(cargar, 350); });
  cat.addEventListener('change', cargar); mod.addEventListener('change', cargar);

  const accion = destino => h('a', { class: 'btn btn-primary btn-sm', href: Session.isLogged() ? 'app.html#/' + destino : 'login.html?tab=registro' },
    Session.isLogged() ? 'Continuar' : 'Registrate para continuar');

  async function cargar() {
    res.innerHTML = '';
    try {
      if (tab === 'cursos' || tab === 'diplomados') {
        const p = await api('/publico/' + tab, { auth: false, query: { q: q.value, categoriaId: cat.value, modalidadId: mod.value, size: 30, sort: 'inicio,asc' } });
        if (!p.content.length) res.appendChild(h('div', { class: 'vacio' }, 'No hay oferta vigente con esos filtros'));
        p.content.forEach(o => res.appendChild(h('div', { class: 'card oferta-card' },
          h('div', { class: 'row' }, badge(o.estado), h('span', { class: 'small muted' }, o.categoriaNombre)),
          h('h3', { style: 'margin-top:.5rem' }, o.nombre),
          o.descripcion ? h('p', { class: 'small' }, o.descripcion) : null,
          h('div', { class: 'meta' }, `${o.modalidadNombre}${o.espacioNombre ? ' · ' + o.espacioNombre : ''} · ${o.docenteNombre}`),
          h('div', { class: 'meta' }, `${fmt.fecha(o.inicio)} al ${fmt.fecha(o.fin)} · ${o.sesiones.length} sesiones · ${o.horasTotales} h`),
          h('div', { class: 'row', style: 'margin-top:.6rem' }, h('span', { class: 'precio' }, fmt.dinero(o.costo)),
            h('span', { class: 'small muted' }, o.cupoDisponible + ' cupos'), h('div', { class: 'spacer' }), o.cupoDisponible > 0 ? accion(tab) : badge('AGOTADO')))));
      } else if (tab === 'espacios') {
        const p = await api('/publico/espacios', { auth: false });
        p.content.forEach(e => res.appendChild(h('div', { class: 'card oferta-card' }, h('span', { class: 'small muted' }, fmt.enum(e.tipo)),
          h('h3', null, e.nombre), h('div', { class: 'meta' }, 'Capacidad: ' + e.capacidad + ' personas'), e.equipamiento ? h('div', { class: 'meta' }, e.equipamiento) : null,
          h('div', { class: 'row' }, h('span', { class: 'precio' }, fmt.dinero(e.precioHora) + ' / hora'), h('div', { class: 'spacer' }), accion('disponibilidad')))));
      } else {
        const p = await api('/publico/servicios-catering', { auth: false });
        p.content.forEach(s => res.appendChild(h('div', { class: 'card oferta-card' }, h('span', { class: 'small muted' }, fmt.enum(s.tipo)),
          h('h3', null, s.nombre), s.menuBase ? h('div', { class: 'meta' }, s.menuBase) : null,
          h('div', { class: 'row' }, h('span', { class: 'precio' }, fmt.dinero(s.precioUnitario) + ' / persona'), h('div', { class: 'spacer' }), accion('catering')))));
      }
    } catch (e) { res.appendChild(alertaError(e)); }
  }
  cargar();
})();
