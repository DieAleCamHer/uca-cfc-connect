# UCA-CFC Connect

Proyecto de Cátedra — Desarrollo de Aplicaciones con Web Frameworks (DWF404)
Universidad Don Bosco — Ciclo II-2026

Sistema de gestión para el Centro de Formación Continua de la UCA: cursos/diplomados,
clientes, inscripciones, cotizaciones, alquiler de espacios, catering, agenda institucional,
pagos y seguridad.

## Estado actual (Bloque 1)

✅ Proyecto Maven configurado
✅ Perfiles `dev` / `prod` con MySQL
✅ Entidades JPA de los 9 módulos
✅ Repositories (15 interfaces JpaRepository)
✅ Services con lógica de negocio (18 servicios: interfaz + implementación)
✅ Controllers REST + DTOs + validaciones + manejo global de errores
✅ Pruebas unitarias con JUnit 5 + Mockito (15 Services, 59 métodos @Test)
✅ Spring Security + JWT + roles
⬜ Spring Security + JWT (Bloque 6)

## Requisitos previos (todo gratuito)

1. **JDK 21** — https://adoptium.net/ (Eclipse Temurin)
2. **MySQL Community Server** — https://dev.mysql.com/downloads/mysql/
3. **IntelliJ IDEA** (Community o Ultimate)
4. **Maven** — viene integrado en IntelliJ, no necesitas instalarlo aparte

## Cómo correrlo

### 1. Preparar MySQL

Abre una terminal de MySQL (o MySQL Workbench) y crea la base de dev:

```sql
CREATE DATABASE ucacfc_dev CHARACTER SET utf8mb4;
```

Si tu usuario/contraseña de MySQL no son `root`/`root`, edita
`src/main/resources/application-dev.properties` con tus credenciales reales.

### 2. Abrir el proyecto en IntelliJ

`File → Open` y selecciona la carpeta `uca-cfc-connect`. IntelliJ detecta el `pom.xml`
automáticamente y descarga las dependencias (necesita internet la primera vez).

### 3. Verificar que Lombok esté habilitado

`File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors` →
marca "Enable annotation processing" (si no lo tienes ya de guías anteriores).

### 4. Ejecutar

Corre la clase `UcaCfcConnectApplication`. Con el perfil `dev` activo (por defecto),
Hibernate va a crear automáticamente todas las tablas en `ucacfc_dev` a partir de las
entidades (`ddl-auto=update`).

Para correr con el perfil `prod`, en IntelliJ: `Run → Edit Configurations →
Environment variables → SPRING_PROFILES_ACTIVE=prod` (y antes crea la base
`ucacfc_prod` igual que la de dev).

### 5. Confirmar que levantó bien

Revisa el log: debe decir `Started UcaCfcConnectApplication`. Entra a MySQL y corre
`SHOW TABLES;` en `ucacfc_dev` — deberías ver las tablas: `usuarios`, `clientes`,
`categorias`, `modalidades`, `docentes`, `cursos`, `diplomados`, `inscripciones`,
`espacios`, `reservas_espacio`, `servicios_catering`, `solicitudes_catering`,
`cotizaciones`, `actividades_agenda`, `pagos`.

Por ahora no hay endpoints REST todavía (eso es el Bloque 4) — este primer bloque
solo deja el modelo de datos funcionando.

## Reglas de negocio implementadas (Bloque 3)

- **No cruces de horario**: `ReservaEspacioService` valida contra su propia tabla, y además
  sincroniza cada reserva con `AgendaService`, que valida cruces contra **toda** la agenda
  institucional (cursos, diplomados, eventos, catering en sitio), no solo alquileres.
- **Cupo máximo**: `InscripcionService` cuenta las inscripciones activas (PENDIENTE/CONFIRMADA)
  de un curso/diplomado y rechaza la inscripción si ya se llenó (`CupoExcedidoException`).
- **Curso XOR Diplomado**: una inscripción debe apuntar a exactamente uno de los dos, nunca
  ambos ni ninguno.
- **Fechas y horas coherentes**: Curso/Diplomado rechazan fecha fin antes de fecha inicio, hora
  fin antes de hora inicio, cupo o costo negativos.
- **Unicidad**: email de Usuario, DUI/NIT de Cliente, nombre de Categoria/Modalidad — todos
  lanzan `RegistroDuplicadoException` si ya existen.
- **Pagos parciales**: `PagoService.abonar(...)` recalcula automáticamente el estado
  (PENDIENTE → PARCIAL → PAGADO) según cuánto se ha abonado del monto total, y rechaza abonos
  que excedan el saldo pendiente.
- **Contraseñas**: nunca se guardan en texto plano — `UsuarioService` las hashea con BCrypt
  antes de persistir.

Todas las excepciones de negocio están en `edu.udb.ucacfc.shared.exception` — en el Bloque 4
se conectan a códigos HTTP reales vía `@ControllerAdvice`.

## Pruebas unitarias (Bloque 5)

Requisito explícito del lineamiento: JUnit 5 + Mockito, probando la lógica de negocio
**sin tocar la base de datos real** (todos los Repository están mockeados con `@Mock`).

Para correrlas en IntelliJ: clic derecho sobre la carpeta `src/test/java` → **Run Tests**.
O desde terminal: `mvn test` (si tienes Maven en el PATH; si no, usa el Maven integrado de
IntelliJ: panel derecho → Maven → `uca-cfc-connect` → Lifecycle → `test`).

| Clase de test | Qué cubre |
|---|---|
| `ReservaEspacioServiceImplTest` | **La regla central del proyecto**: crea/rechaza reservas según cruce de horario, valida fechas, sincroniza (o no) con la agenda |
| `InscripcionServiceImplTest` | Cupo máximo, regla curso XOR diplomado |
| `PagoServiceImplTest` | Cálculo automático de estado (PENDIENTE → PARCIAL → PAGADO), rechazo de abonos que exceden el saldo |
| `AgendaServiceImplTest` | Cruce de horario a nivel de agenda institucional completa, actividades sin espacio físico |
| `ClienteServiceImplTest` | Unicidad de DUI/NIT, recurso no encontrado |
| `CursoServiceImplTest` | Validaciones de fechas/horas/cupo/costo |

Si tu equipo necesita más pruebas para otros módulos (Cotización, Catering, Usuario), el
patrón es siempre el mismo: `@ExtendWith(MockitoExtension.class)`, `@Mock` para cada
Repository que use el Service, `@InjectMocks` para el Service, y `when(...).thenReturn(...)`
para simular lo que devolvería la base de datos.

## Frontend visual (HTML/CSS/JS, 100% gratis)

Se agregó un frontend real —no solo Swagger— porque el equipo confirmó que sí se pide. Vive
en `src/main/resources/static/` y **Spring Boot lo sirve automáticamente**, sin servidor
aparte, sin Node.js, sin npm: al arrancar la app, entra directo a **http://localhost:8080/**.

### Cómo está construido

En vez de escribir una pantalla distinta para cada uno de los 9 módulos (mucho código
repetido y difícil de mantener entre 5 personas), se hizo un **motor CRUD genérico**:

- `js/config.js` — declara los 15 módulos: qué campos tiene cada uno, qué endpoint usan, qué
  rol puede leer/crear/editar/borrar cada uno (**refleja exactamente** la matriz de
  `SecurityConfig.java` del backend — si cambian los roles allá, hay que reflejarlo aquí).
- `js/crud.js` — el motor: lee esa configuración y genera la tabla, el formulario modal, y
  las validaciones. Esto es lo que hace que agregar un módulo nuevo a futuro sea agregar un
  objeto en `config.js`, no escribir HTML a mano.
- `js/api.js` — un solo lugar que agrega el JWT a cada request y traduce errores del backend.
- `js/app.js` — arma el menú lateral según el rol de quien inició sesión (un CLIENTE no ve
  "Usuarios", un CONTABILIDAD sí ve "Pagos", etc.)
- `css/theme.css` — identidad visual institucional (azul oscuro / dorado / blanco, colores
  oficiales de la UCA según su Wikipedia/manual de identidad visual: negro, blanco y azul).

### Estilo y diseño

Nota importante: no se reprodujo el logo real de la UCA (evitar temas de derechos de autor
sobre la marca institucional) — es un diseño **inspirado** en su paleta de colores, con
tipografía y estructura de panel administrativo profesional (sidebar + tarjetas + tablas).

### Módulos con comportamiento especial

- **Inscripciones / Cotizaciones / Solicitudes de catering**: no tienen botón "Editar" (el
  backend no expone PUT para ellos, solo `PATCH .../estado`) — en su lugar hay un selector de
  "Cambiar estado" por fila.
- **Reservas de espacio**: el botón de borrar dice "Cancelar" (coincide con lo que hace el
  backend). Si intentas crear una reserva con horario cruzado, el error de `EspacioOcupadoException`
  se muestra tal cual en pantalla.
- **Agenda**: solo lectura, con selector de rango de fechas (el endpoint lo exige).
- **Pagos**: se busca por estado o por referencia (no hay "listar todos" en el backend), y
  cada fila con saldo pendiente tiene un botón "Abonar".
- **Usuarios**: al editar no pide contraseña (se agregó `UsuarioUpdateRequestDTO` en el
  backend específicamente para esto, sin campo password).

### Cómo probarlo

1. Corre la app normalmente (`UcaCfcConnectApplication`)
2. Entra a **http://localhost:8080/**
3. Pestaña "Crear cuenta" para un CLIENTE de prueba, o usa tu usuario ADMIN si ya lo creaste
4. Inicia sesión — el menú lateral se arma solo según tu rol

## Seguridad JWT y roles (Bloque 6)

Se implementó con la técnica estándar de la industria (la misma que enseña Baeldung, uno de
tus recursos): `Usuario implements UserDetails` directamente, `UsuarioDetailsServiceImpl`,
`JwtService` (genera/valida tokens con la librería jjwt), y `JwtAuthenticationFilter` que
intercepta cada request. **Se implementó ya, sin esperar la guía oficial de Unidad 4** — si el
estilo de la profesora difiere en algo (nombres, estructura de paquetes), es un ajuste menor
sobre una base que ya funciona y está probada.

### Cómo hacer login

```
POST /api/auth/login
{
  "email": "admin@ucacfc.com",
  "password": "clave123"
}
```

Responde con un `token` que debes mandar en cada request protegido:
`Authorization: Bearer <token>`. En Swagger UI, hay un botón **Authorize** (candado) donde
pegas `Bearer <token>` una sola vez y ya queda aplicado a todos los endpoints que pruebes ahí.

### Cómo crear el primer usuario (ADMIN)

Como `/api/usuarios` ahora requiere ser ADMIN para crear usuarios, pero el primer ADMIN no
existe todavía, usa el registro público (que siempre crea rol CLIENTE)... **excepto** que
para el primer usuario ADMIN necesitas insertarlo directo en MySQL una vez:

```sql
-- La contrasena de este INSERT es un hash BCrypt de "admin123", generado aparte.
-- Mas facil: usa /api/auth/registro para crear un CLIENTE de prueba,
-- y en MySQL simplemente actualizale el rol:
UPDATE usuarios SET rol = 'ADMIN' WHERE email = 'tu-primer-usuario@correo.com';
```

O, si prefieres no tocar MySQL a mano: comenta TEMPORALMENTE la línea
`.requestMatchers("/api/usuarios/**").hasRole("ADMIN")` en `SecurityConfig`, crea tu primer
ADMIN con `POST /api/usuarios`, y vuelve a descomentarla.

### Matriz de roles implementada

| Ruta | Quién puede |
|---|---|
| `/api/auth/**` | Público (nadie necesita token) |
| `/api/usuarios/**` | Solo ADMIN |
| Catálogos (`categorias`, `modalidades`, `docentes`, `cursos`, `diplomados`, `espacios`, `servicios-catering`) — GET | Cualquier usuario autenticado |
| Catálogos — POST/PUT | ADMIN, RECEPCIONISTA |
| Catálogos — DELETE | Solo ADMIN |
| `clientes`, `inscripciones`, `reservas-espacio`, `solicitudes-catering`, `cotizaciones` | ADMIN, RECEPCIONISTA, CLIENTE |
| `agenda` (solo lectura) | Cualquier usuario autenticado |
| `pagos` | ADMIN, CONTABILIDAD |

Esta matriz es un punto de partida razonable y defendible, pero **tu equipo la puede ajustar**
según lo que pida la guía oficial — está toda centralizada en `SecurityConfig.java`, en un
solo lugar, precisamente para que sea fácil de modificar sin tocar los Controllers.

### Nota de diseño importante para tu defensa

`POST /api/auth/registro` (auto-registro público) **siempre** fuerza el rol a `CLIENTE`, sin
importar qué rol venga en el JSON — así nadie puede auto-registrarse como ADMIN. Crear cuentas
de personal (RECEPCIONISTA, CONTABILIDAD, ADMIN) solo lo puede hacer un ADMIN ya autenticado,
vía `POST /api/usuarios`. Es una decisión de seguridad real (principio de mínimo privilegio),
no solo un requisito académico — vale la pena mencionarla en la defensa.

## Probar la API (Bloque 4)

Con la app corriendo, entra a **http://localhost:8080/swagger-ui.html** — ahí ves y pruebas
TODOS los endpoints sin instalar nada más (Swagger UI viene incluido, gratis).

Nota temporal: `SecurityConfig` deja todo abierto (`permitAll`) porque `spring-boot-starter-security`
ya está en el `pom.xml` mirando hacia el Bloque 6, pero JWT real todavía no existe. Sin esa
clase, Spring Security bloquearía todo con Basic Auth automático.

### Endpoints principales

| Módulo | Base path | Notas |
|---|---|---|
| Usuarios | `/api/usuarios` | password se hashea con BCrypt |
| Clientes | `/api/clientes` | `?nombre=` para buscar |
| Categorías | `/api/categorias` | catálogo |
| Modalidades | `/api/modalidades` | catálogo |
| Docentes | `/api/docentes` | catálogo |
| Cursos | `/api/cursos` | `?soloActivos=true` |
| Diplomados | `/api/diplomados` | `?soloActivos=true` |
| Inscripciones | `/api/inscripciones` | `POST` valida cupo; `PATCH /{id}/estado?estado=` |
| Espacios | `/api/espacios` | `?soloDisponibles=true` |
| Reservas de espacio | `/api/reservas-espacio` | **el endpoint clave**: prueba crear 2 reservas del mismo espacio con horario cruzado → la 2ª debe dar 409 |
| Servicios de catering | `/api/servicios-catering` | catálogo |
| Solicitudes de catering | `/api/solicitudes-catering` | `PATCH /{id}/estado?estado=` |
| Cotizaciones | `/api/cotizaciones` | `PATCH /{id}/estado?estado=` |
| Agenda | `/api/agenda?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | solo lectura |
| Pagos | `/api/pagos` | `PATCH /{id}/abonar` recalcula el estado solo |

### Orden recomendado para probar de punta a punta

1. Crear una `Categoria`, una `Modalidad` y un `Docente`
2. Crear un `Curso` referenciando esos 3 ids
3. Crear un `Cliente`
4. `POST /api/inscripciones` con `clienteId` + `cursoId`
5. Crear un `Espacio`
6. `POST /api/reservas-espacio` dos veces con el mismo `espacioId` y horarios que se crucen →
   confirma el 409 con mensaje de `EspacioOcupadoException`
7. `GET /api/agenda?desde=...&hasta=...` → deberías ver la actividad reflejada ahí también

## Estructura de paquetes

```
edu.udb.ucacfc
 ├── shared       → utilidades comunes (auditoría automática, futuras excepciones globales)
 ├── seguridad    → Usuario, Rol
 ├── cliente      → Cliente
 ├── academico    → Categoria, Modalidad, Docente, Curso, Diplomado
 ├── inscripcion  → Inscripcion
 ├── cotizacion   → Cotizacion
 ├── espacio      → Espacio, ReservaEspacio
 ├── catering     → ServicioCatering, SolicitudCatering
 ├── agenda       → ActividadAgenda (tabla central para validar cruces de horario)
 └── pago         → Pago
```
