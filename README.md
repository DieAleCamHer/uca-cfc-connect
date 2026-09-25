# UCA-CFC Connect

Proyecto de Cátedra — Desarrollo de Aplicaciones con Web Frameworks (DWF404), Universidad Don Bosco, Ciclo II-2026.

Es un sistema de gestión para el Centro de Formación Continua de la UCA. Maneja cursos y diplomados, clientes, inscripciones, cotizaciones, alquiler de espacios, catering, agenda institucional, pagos y seguridad.

## Estado actual

Hasta ahora el proyecto tiene:

- Proyecto Maven configurado, con perfiles `dev` y `prod` para MySQL
- Las entidades JPA de los 9 módulos
- 15 interfaces Repository
- 18 servicios (interfaz + implementación) con la lógica de negocio
- Controllers REST, DTOs, validaciones y manejo global de errores
- Pruebas unitarias con JUnit 5 y Mockito (15 servicios, 59 métodos de test)
- Seguridad con Spring Security + JWT + roles

Lo único que falta terminar es afinar algunos detalles de seguridad del Bloque 6.

## Qué necesitas para correrlo

Todo es gratis:

1. JDK 21 — https://adoptium.net/ (Eclipse Temurin)
2. MySQL Community Server — https://dev.mysql.com/downloads/mysql/
3. IntelliJ IDEA (sirve la versión Community)
4. Maven ya viene integrado en IntelliJ, no hay que instalarlo aparte

## Cómo levantarlo

### 1. Preparar la base de datos

Desde una terminal de MySQL o MySQL Workbench:

```sql
CREATE DATABASE ucacfc_dev CHARACTER SET utf8mb4;
```

Si tu usuario o contraseña de MySQL no son `root`/`root`, cambia eso en `src/main/resources/application-dev.properties`.

### 2. Abrir el proyecto

`File → Open` en IntelliJ y seleccionas la carpeta `uca-cfc-connect`. Va a detectar el `pom.xml` solo y descargar las dependencias (necesitas internet la primera vez que lo abras).

### 3. Revisar que Lombok esté habilitado

En `File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors`, marca "Enable annotation processing" si no lo tenías ya activado.

### 4. Ejecutar

Corres la clase `UcaCfcConnectApplication`. Con el perfil `dev` (que es el que está activo por defecto), Hibernate crea automáticamente las tablas en `ucacfc_dev` a partir de las entidades.

Si quieres correr con el perfil `prod`, vas a `Run → Edit Configurations → Environment variables` y pones `SPRING_PROFILES_ACTIVE=prod` (y antes creas la base `ucacfc_prod` igual que hiciste con la de dev).

### 5. Confirmar que arrancó bien

En el log debería aparecer `Started UcaCfcConnectApplication`. Si entras a MySQL y corres `SHOW TABLES;` en `ucacfc_dev`, deberías ver: `usuarios`, `clientes`, `categorias`, `modalidades`, `docentes`, `cursos`, `diplomados`, `inscripciones`, `espacios`, `reservas_espacio`, `servicios_catering`, `solicitudes_catering`, `cotizaciones`, `actividades_agenda`, `pagos`.

Todavía no hay endpoints REST en esta parte (eso viene en el Bloque 4), por ahora solo queda funcionando el modelo de datos.

## Reglas de negocio

Algunas de las validaciones más importantes que ya están implementadas:

- **Sin cruces de horario**: `ReservaEspacioService` revisa contra su propia tabla y además sincroniza con `AgendaService`, que valida contra toda la agenda institucional (no solo contra los alquileres de espacio).
- **Cupo máximo**: `InscripcionService` cuenta las inscripciones activas de un curso o diplomado y rechaza si ya no hay cupo, lanzando `CupoExcedidoException`.
- **Curso o Diplomado, no ambos**: una inscripción tiene que apuntar a uno de los dos, nunca a los dos ni a ninguno.
- **Fechas y horas coherentes**: no se puede poner una fecha fin antes que la de inicio, ni cupo o costo en negativo.
- **Sin duplicados**: email de usuario, DUI/NIT de cliente, nombre de categoría o modalidad, todos lanzan `RegistroDuplicadoException` si ya existen.
- **Pagos parciales**: `PagoService.abonar(...)` recalcula el estado del pago (PENDIENTE → PARCIAL → PAGADO) según lo que se ha abonado, y no deja abonar más de lo que falta.
- **Contraseñas**: nunca se guardan en texto plano, se hashean con BCrypt antes de guardarlas.

Todas las excepciones de negocio están en `edu.udb.ucacfc.shared.exception`. En el Bloque 4 se conectan a códigos HTTP reales con `@ControllerAdvice`.

## Pruebas unitarias

El lineamiento pedía JUnit 5 + Mockito probando la lógica sin tocar la base de datos real, así que todos los Repository están mockeados con `@Mock`.

Para correrlas desde IntelliJ: clic derecho en `src/test/java` → Run Tests. O desde terminal con `mvn test` (si no tienes Maven en el PATH, usa el Maven integrado de IntelliJ: panel derecho → Maven → uca-cfc-connect → Lifecycle → test).

| Clase de test | Qué prueba |
|---|---|
| `ReservaEspacioServiceImplTest` | La regla central del proyecto: crea o rechaza reservas según cruce de horario, valida fechas y si sincroniza con la agenda |
| `InscripcionServiceImplTest` | Cupo máximo, regla de curso o diplomado |
| `PagoServiceImplTest` | Cálculo automático del estado del pago, rechazo de abonos de más |
| `AgendaServiceImplTest` | Cruces de horario a nivel de toda la agenda institucional |
| `ClienteServiceImplTest` | Unicidad de DUI/NIT, recurso no encontrado |
| `CursoServiceImplTest` | Validaciones de fechas, horas, cupo y costo |

Si hace falta agregar pruebas para otros módulos (Cotización, Catering, Usuario), el patrón es siempre el mismo: `@ExtendWith(MockitoExtension.class)`, un `@Mock` por cada Repository que use el Service, `@InjectMocks` para el Service, y `when(...).thenReturn(...)` simulando lo que devolvería la base de datos.

## Frontend

Se agregó un frontend real (no solo Swagger) porque el equipo confirmó que sí se pedía. Está en `src/main/resources/static/` y Spring Boot lo sirve solo, sin servidor aparte ni Node ni npm: al arrancar la app entras directo a http://localhost:8080/.

### Cómo está armado

En vez de hacer una pantalla distinta para cada uno de los 9 módulos, se hizo un motor CRUD genérico:

- `js/config.js` declara los 15 módulos: qué campos tiene cada uno, qué endpoint usan, y qué rol puede leer, crear, editar o borrar cada uno. Esto tiene que reflejar lo mismo que está en `SecurityConfig.java` del backend, así que si cambian los roles allá hay que actualizarlo aquí también.
- `js/crud.js` es el motor: lee la configuración de arriba y genera la tabla, el formulario y las validaciones. Así, agregar un módulo nuevo es agregar un objeto en `config.js`, no escribir HTML a mano.
- `js/api.js` centraliza el JWT en cada request y traduce los errores que manda el backend.
- `js/app.js` arma el menú lateral según el rol de quien inició sesión (un CLIENTE no ve "Usuarios", pero CONTABILIDAD sí ve "Pagos").
- `css/theme.css` tiene los colores institucionales (azul oscuro, dorado, blanco).

No se usó el logo real de la UCA para evitar temas de derechos de la marca, es un diseño inspirado en su paleta de colores.

### Casos especiales en el frontend

- Inscripciones, cotizaciones y solicitudes de catering no tienen botón de editar (el backend no expone PUT para esos, solo PATCH del estado), en su lugar hay un selector para cambiar estado.
- En reservas de espacio, el botón de borrar dice "Cancelar". Si intentas crear una reserva con horario cruzado, sale el error de `EspacioOcupadoException` directo en pantalla.
- Agenda es de solo lectura, con un selector de rango de fechas.
- Pagos se busca por estado o por referencia, no hay "listar todos" en el backend. Las filas con saldo pendiente tienen botón de "Abonar".
- Usuarios no pide contraseña al editar (se agregó `UsuarioUpdateRequestDTO` en el backend sin ese campo).

### Cómo probarlo

1. Corre la app (`UcaCfcConnectApplication`)
2. Entra a http://localhost:8080/
3. Crea una cuenta de CLIENTE de prueba, o usa tu ADMIN si ya lo tienes
4. Inicia sesión y el menú se arma solo según tu rol

## Seguridad con JWT

Se implementó con `Usuario implements UserDetails`, `UsuarioDetailsServiceImpl`, `JwtService` (con la librería jjwt) y `JwtAuthenticationFilter` que intercepta cada request. Se hizo antes de la guía oficial de la Unidad 4, así que si el estilo de la profesora pide otra estructura, es un ajuste sobre algo que ya funciona y está probado.

### Login

```
POST /api/auth/login
{
  "email": "admin@ucacfc.com",
  "password": "clave123"
}
```

Devuelve un `token` que hay que mandar en cada request protegido: `Authorization: Bearer <token>`. En Swagger UI hay un botón Authorize donde pegas `Bearer <token>` una vez y queda aplicado a todos los endpoints.

### Crear el primer usuario ADMIN

Como `/api/usuarios` ahora exige ser ADMIN para crear usuarios, pero el primer ADMIN todavía no existe, hay que hacer un paso manual. Lo más fácil es crear un CLIENTE de prueba con `/api/auth/registro` y luego en MySQL:

```sql
UPDATE usuarios SET rol = 'ADMIN' WHERE email = 'tu-primer-usuario@correo.com';
```

Otra opción, si no quieres tocar MySQL directo, es comentar temporalmente la línea `.requestMatchers("/api/usuarios/**").hasRole("ADMIN")` en `SecurityConfig`, crear el primer ADMIN con `POST /api/usuarios`, y volver a descomentarla.

### Roles por ruta

| Ruta | Quién puede |
|---|---|
| `/api/auth/**` | Público, sin token |
| `/api/usuarios/**` | Solo ADMIN |
| Catálogos (categorías, modalidades, docentes, cursos, diplomados, espacios, servicios de catering) — GET | Cualquier usuario autenticado |
| Catálogos — POST/PUT | ADMIN, RECEPCIONISTA |
| Catálogos — DELETE | Solo ADMIN |
| Clientes, inscripciones, reservas de espacio, solicitudes de catering, cotizaciones | ADMIN, RECEPCIONISTA, CLIENTE |
| Agenda (solo lectura) | Cualquier usuario autenticado |
| Pagos | ADMIN, CONTABILIDAD |

Esto está centralizado en `SecurityConfig.java`, así que si el equipo necesita ajustarlo según lo que pida la guía oficial, se cambia en un solo lugar sin tocar los Controllers.

Un detalle que vale la pena mencionar en la defensa: `POST /api/auth/registro` (el auto-registro público) siempre fuerza el rol a CLIENTE sin importar qué rol venga en el JSON, para que nadie pueda auto-registrarse como ADMIN. Crear cuentas de personal (RECEPCIONISTA, CONTABILIDAD, ADMIN) solo lo puede hacer un ADMIN ya logueado, vía `POST /api/usuarios`.

## Probar la API

Con la app corriendo, entras a http://localhost:8080/swagger-ui.html y ahí puedes ver y probar todos los endpoints sin instalar nada más.

Nota: por ahora `SecurityConfig` deja todo abierto con `permitAll`, porque la dependencia de seguridad ya está en el `pom.xml` pensando en el Bloque 6 pero sin JWT real todavía no se puede exigir login en todo.

### Endpoints

| Módulo | Base path | Notas |
|---|---|---|
| Usuarios | `/api/usuarios` | password se hashea con BCrypt |
| Clientes | `/api/clientes` | `?nombre=` para buscar |
| Categorías | `/api/categorias` | catálogo |
| Modalidades | `/api/modalidades` | catálogo |
| Docentes | `/api/docentes` | catálogo |
| Cursos | `/api/cursos` | `?soloActivos=true` |
| Diplomados | `/api/diplomados` | `?soloActivos=true` |
| Inscripciones | `/api/inscripciones` | POST valida cupo; `PATCH /{id}/estado?estado=` |
| Espacios | `/api/espacios` | `?soloDisponibles=true` |
| Reservas de espacio | `/api/reservas-espacio` | el endpoint más importante: si creas dos reservas del mismo espacio con horario cruzado, la segunda debe dar 409 |
| Servicios de catering | `/api/servicios-catering` | catálogo |
| Solicitudes de catering | `/api/solicitudes-catering` | `PATCH /{id}/estado?estado=` |
| Cotizaciones | `/api/cotizaciones` | `PATCH /{id}/estado?estado=` |
| Agenda | `/api/agenda?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | solo lectura |
| Pagos | `/api/pagos` | `PATCH /{id}/abonar` recalcula el estado solo |

### Orden sugerido para probar todo de una vez

1. Crear una Categoría, una Modalidad y un Docente
2. Crear un Curso referenciando esos ids
3. Crear un Cliente
4. `POST /api/inscripciones` con `clienteId` y `cursoId`
5. Crear un Espacio
6. `POST /api/reservas-espacio` dos veces con el mismo `espacioId` y horarios cruzados, para confirmar el 409 con el mensaje de `EspacioOcupadoException`
7. `GET /api/agenda?desde=...&hasta=...` para ver que la actividad también se refleja ahí

## Estructura de paquetes

```
edu.udb.ucacfc
 ├── shared       → utilidades comunes (auditoría automática, excepciones globales)
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
