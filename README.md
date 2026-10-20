# UCA-CFC Connect 2.0 — Proyecto de cátedra DWF404

Sistema web del **Centro de Formación Continua de la UCA**: oferta académica, clientes, inscripciones,
cotizaciones, alquiler de espacios, catering, agenda institucional, pagos y seguridad.

**Stack:** Spring Boot 3.3.4 · Java 21 · Maven · Spring Data JPA / Hibernate · MySQL · Spring Security + JWT (jjwt 0.12.6)
· springdoc-openapi 2.6.0 · JUnit 5 + Mockito · Frontend HTML/CSS/JS puro (lo sirve el backend o Live Server, con CORS habilitado).

---

## 1. Cómo correrlo

1. **MySQL** encendido en `localhost:3306`. La base `ucacfc` se crea sola (`createDatabaseIfNotExist=true`) y las tablas
   las crea Hibernate (`ddl-auto=update`). Si tu usuario/clave no es `root/root`, cámbialo en
   `src/main/resources/application.properties` o con las variables `DB_USERNAME` / `DB_PASSWORD`.
   > Si vienes de la versión anterior, usa una base nueva (`ucacfc`) — el modelo cambió.
2. Abrir la carpeta en IntelliJ → recargar Maven → ejecutar `UcaCfcConnectApplication`.
3. Abrir **http://localhost:8080** (frontend) o **http://localhost:8080/swagger-ui.html** (API).
4. *(Opcional, igual que el Desafío 3)* Abrir `src/main/resources/static` en VS Code → clic derecho en `index.html` →
   **Open with Live Server** (`http://127.0.0.1:5500`). El frontend detecta que no está en el puerto 8080 y llama a
   `http://localhost:8080` (cambiar `BACKEND_REMOTO` en `js/config.js` si el backend está en otra dirección); el
   navegador aplica CORS y el backend lo permite con `CorsConfig`.

Al primer arranque se crea el administrador y, si la base está vacía, datos de ejemplo (catálogos, 2 cursos, 1 diplomado,
5 espacios, 5 servicios de catering, 1 cliente persona con cuenta y 1 cliente empresa de mostrador):

| Rol | Correo | Contraseña |
|---|---|---|
| ADMIN | admin@ucacfc.edu.sv | Admin12345 |
| RECEPCIONISTA | recepcion@ucacfc.edu.sv | Recepcion2026 |
| CONTABILIDAD | contabilidad@ucacfc.edu.sv | Conta2026 |
| CLIENTE | ana.perez@correo.com | Cliente2026 |

Pruebas: clic derecho en `src/test/java` → *Run Tests* (Mockito puro, no necesitan MySQL).
Entrega: `mvn clean` y comprimir la carpeta raíz (la que tiene `pom.xml`).

### Swagger con candado
1. `POST /api/auth/login` (no lleva candado) → copiar `accessToken`.
2. Botón **Authorize** → pegar **solo el token** (Swagger agrega `Bearer `). `persist-authorization` lo conserva al recargar.
3. Cada endpoint muestra en su descripción qué rol lo usa, qué reglas aplica, los campos de filtro y los campos válidos para `sort`.
   Las respuestas de error (400/401/403/404/409/500) se documentan solas con el esquema `ErrorResponse`.

---

## 2. Arquitectura

```
Controller  (REST, @Valid, @PreAuthorize por rol, sin lógica)
   └─ Service  (reglas de negocio, @Transactional, dueño del registro, cálculo de costos)
        └─ Repository  (JpaRepository + JpaSpecificationExecutor: filtros dinámicos)
             └─ Entity → MySQL
Entra un RequestDTO (record con Bean Validation), sale un ResponseDTO. La entidad nunca sale por la API.
```

| Paquete | Contenido |
|---|---|
| `shared` | excepciones + `GlobalExceptionHandler`, paginación (`PaginaRequest`, `PageResponse`, `PaginacionUtil`), `MaquinaEstados`, `Horarios`, Swagger, `Clock`, datos iniciales, `AccesoService` (dueño del registro), `Permisos` (expresiones de `@PreAuthorize`) |
| `seguridad` | `Usuario`/`Rol`, JWT (filtro, servicio, 401/403 JSON), `SecurityConfig`, auth (login, registro, refresh, logout, recuperación), administración de usuarios y roles |
| `academico` | categorías, modalidades, docentes, cursos y diplomados (lógica común en `OfertaAcademicaService`) |
| `cliente` | fichas de cliente (persona/empresa), acceso web, historial (RF14) |
| `inscripcion` | inscripciones + cascada al dar de baja una oferta |
| `espacio` | espacios, disponibilidad (RF09) y reservas/alquileres (RF07) |
| `catering` | servicios y solicitudes (RF08) |
| `cotizacion` | cotizaciones con líneas (RF05, RF06) |
| `agenda` | calendario institucional + eventos del CFC (RF13) |
| `pago` | cuentas por cobrar, abonos, validación, comprobantes (RF10, RF15) |
| `cierre` | cierre y vencimiento automáticos (tarea cada 15 min) |
| `reporte` | resumen operativo y estado financiero |
| `publico` | oferta vigente sin token para la página de inicio |

---

## 3. Seguridad en tres capas

| Capa | Dónde | Responde | Ejemplo |
|---|---|---|---|
| 1. Token | `JwtAuthenticationFilter` | 401 con `code` | `NO_TOKEN`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `TOKEN_WRONG_TYPE`, `TOKEN_REVOKED`, `ACCOUNT_DISABLED` |
| 2. Rol | `@PreAuthorize(Permisos.X)` en cada método | 403 `ACCESS_DENIED` | RECEPCIONISTA intenta `PATCH /api/cotizaciones/{id}/aprobar` |
| 3. Dueño | `AccesoService` en los services | 403 `FORBIDDEN_OPERATION` | un CLIENTE pide `GET /api/reservas/7` de otro cliente |

- `SecurityConfig` solo define lo público (`/api/auth/login|registro|refresh|recuperar|restablecer`, `/api/publico/**`,
  Swagger y el frontend estático) y que el resto de `/api/**` exige token. **El rol lo decide `@PreAuthorize`**: así cada
  regla se lee junto al endpoint y los services no repiten `if (rol == ...)`.
- Acciones de estado como endpoints separados (`/confirmar`, `/aprobar`, `/rechazar`...) para que cada una tenga su propio
  `@PreAuthorize`; el service solo valida la máquina de estados y el dueño.
- **Access token** 30 min + **refresh token** 7 días (`POST /api/auth/refresh`). El frontend renueva solo al recibir `TOKEN_EXPIRED`.
- **Logout real**: `tokenVersion` en el usuario; logout, cambio de contraseña, cambio de rol o desactivación lo incrementan y
  todos los tokens anteriores quedan revocados al instante. El rol se relee de la BD en cada petición.
- **Recuperación de contraseña**: token aleatorio de un solo uso (30 min), en BD solo se guarda su SHA-256; la respuesta es
  igual exista o no el correo. Sin servidor de correo, en modo académico el token se devuelve como `tokenDemo` y se escribe en el log.
- Login: una cuenta desactivada solo se informa **después** de comprobar la contraseña (no revela qué cuentas existen).
- El registro público **siempre** crea rol CLIENTE (si alguien envía `"rol"`, Jackson responde 400 por campo desconocido).
- Contraseñas BCrypt, mínimo 8 caracteres con letras y números.
- **CORS** (`seguridad/CorsConfig` + `.cors(Customizer.withDefaults())` en `SecurityConfig`), igual que en el Desafío 3:
  - El bean se llama `corsConfigurationSource`: así Spring Security lo encuentra y pone el `CorsFilter` **al principio**
    de la cadena. Los preflight `OPTIONS` (que el navegador manda sin token) se responden antes del filtro JWT, y las
    respuestas 401/403 también llevan las cabeceras CORS, de modo que el frontend puede leer el JSON de error.
  - Métodos GET, POST, PUT, PATCH, DELETE, OPTIONS; headers `Authorization`, `Content-Type`, `Accept`; preflight cacheado 1 h.
  - `allowCredentials(false)` porque el token va en un header y no en cookies, por eso se permite `*`. Para restringir:
    `app.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500` (o la variable `CORS_ORIGINS`).
  - `OPTIONS /**` con `permitAll` queda como respaldo.

### Matriz de permisos

| Módulo | ADMIN | RECEPCIONISTA | CLIENTE | CONTABILIDAD |
|---|---|---|---|---|
| Usuarios y roles | todo | — | — | — |
| Categorías, modalidades, docentes, espacios, servicios | todo | ver | ver | ver |
| Cursos / diplomados | todo | ver, crear, editar | ver | ver |
| Clientes | todo | ver, crear, editar, crear acceso | su ficha (`/me`) e historial | — |
| Inscripciones, reservas, catering | todo | todo menos lo de ADMIN | crear, ver, editar y cancelar **lo suyo** | — |
| Cotizaciones | todo, **aprobar** | registrar, elaborar, rechazar | solicitar, editar (pendiente), retirar lo suyo | — |
| Agenda / eventos | ver, programar eventos | ver, programar eventos | ver (sin alquileres/catering ajenos) | — |
| Pagos | todo | ver, registrar abonos (efectivo/tarjeta quedan confirmados) | ver lo suyo, reportar transferencia/depósito | ver, registrar, **validar**, anular, comprobantes |
| Reportes | resumen y financiero | — | — | financiero |

---

## 4. Reglas de negocio por módulo

> Marcadas con ➕ las que **no pide el documento** pero se agregaron para que el sistema sea coherente en todos los casos.

### Académico
- **Categoría / modalidad**: nombre único (sin mayúsculas). Desactivar no afecta ofertas existentes; impide usarlas en ofertas nuevas.
- ➕ **Modalidad**: no se cambia `requiereEspacioFisico` si tiene ofertas vigentes (dejaría cursos virtuales con aula).
- **Docente**: correo y teléfono únicos. ➕ No se desactiva con ofertas vigentes asignadas.
- **Curso / diplomado** (lógica compartida):
  - 1 a 200 **sesiones** de 30 min a 12 h, que no se cruzan entre sí; todas futuras al crear. `inicio`/`fin` de la oferta son el resumen.
  - Categoría, modalidad y docente existen y están activos (un catálogo desactivado se acepta solo si la oferta ya lo tenía).
  - **Presencial** → espacio obligatorio, activo, con capacidad ≥ cupo. **Virtual** → no puede tener espacio (400).
  - Nombre + categoría + fecha de inicio únicos (otra fecha = otra edición).
  - **Docente**: no puede tener otra oferta vigente con sesiones cruzadas (409 `TEACHER_BUSY`); las ofertas desactivadas o terminadas no lo ocupan. ➕ Se bloquea la fila del docente al validar (concurrencia).
  - **Espacio**: se registra en la agenda **una vez por sesión**; si una sola choca, no se guarda nada (409 `SPACE_OCCUPIED`).
  - ➕ Editar: antes de iniciar se puede cambiar todo (se reprograma la agenda); ya iniciada solo nombre, descripción, cupo y costo; terminada o desactivada es solo lectura. El cupo nunca baja de las inscripciones vigentes. El costo nuevo solo aplica a inscripciones futuras.
  - **Desactivar** (ADMIN) antes de terminar: cancela inscripciones vigentes, anula sus cuentas y libera la agenda — **todo o nada**: si una tiene dinero cobrado o en validación, 409 y no cambia nada. Ya terminada: solo se oculta.
  - **Reactivar**: revalida catálogos, docente y espacio y vuelve a ocupar la agenda; las inscripciones canceladas no vuelven.

### Clientes
- Tipo **PERSONA** (DUI o NIT) o **EMPRESA** (NIT de 14 dígitos + razón social). DUI/NIT, correo y teléfono únicos.
- ➕ El tipo no se cambia después de creado.
- ➕ Una cuenta CLIENTE siempre va unida a una ficha: el registro público crea las dos en una transacción; un cliente de mostrador recibe acceso con `POST /api/clientes/{id}/cuenta`.
- ➕ No se desactiva un cliente con procesos vigentes o saldo pendiente; desactivarlo bloquea su cuenta de acceso.
- **Historial (RF14)**: conteo por estado de cada módulo, facturado/pagado/saldo y últimos movimientos.

### Inscripciones
- Curso **o** diplomado (exactamente uno); cliente y oferta activos; la oferta no terminó.
- ➕ **Participantes**: una PERSONA se inscribe a sí misma; una EMPRESA inscribe colaboradores (nombre + DUI), cada uno una vez por oferta.
- **Cupo**: PENDIENTE y CONFIRMADA ocupan cupo (409 `CAPACITY_EXCEEDED`). ➕ La fila de la oferta se bloquea al contar: dos inscripciones simultáneas no superan el cupo.
- ➕ El **monto se congela** al inscribirse.
- `PENDIENTE → CONFIRMADA → FINALIZADA`; `PENDIENTE/CONFIRMADA → CANCELADA`.
  Confirmar (personal) **genera la cuenta por cobrar**. Finalizar exige oferta terminada y cuenta PAGADA.
  ➕ Un CLIENTE cancela solo antes de que inicie la oferta. Editar solo en PENDIENTE (cambiar de oferta o participante).

### Alquiler de espacios
- Espacio activo, futura, de 30 min a 12 h, asistentes ≤ capacidad. **Costo = horas × precio por hora** (calculado y congelado).
- La **agenda** valida que no choque con cursos, diplomados, eventos, otras reservas ni catering.
- `SOLICITADA` ya aparta el espacio → `CONFIRMADA` (genera la cuenta) → `COMPLETADA` (horario terminado + pagada);
  `SOLICITADA → RECHAZADA` (motivo); `SOLICITADA/CONFIRMADA → CANCELADA`.
- ➕ El CLIENTE cancela hasta 24 h antes; el personal hasta el inicio. No se cancela con un catering vigente dentro.
- **Disponibilidad (RF09)**: libre/ocupado por espacio, actividad que lo ocupa y costo estimado.

### Catering
- ➕ Se pide con **24 h de anticipación**; mismo día, máximo 8 h. **Total = precio por persona × asistentes** (lo calcula el service).
- ➕ Lugar: dentro de una **reserva** del mismo cliente (no vuelve a bloquear el espacio), en un **espacio libre** del CFC (lo bloquea), o **externo**.
  Con espacio, asistentes ≤ capacidad.
- `PENDIENTE → CONFIRMADA → ATENDIDA`; `PENDIENTE → RECHAZADA`; `PENDIENTE/CONFIRMADA → CANCELADA`.

### Cotizaciones
- ➕ Con **líneas** que referencian el catálogo (curso/diplomado por participante, espacio por hora, catering por persona, u OTRO).
  El tipo define qué líneas admite; **servicio combinado** exige al menos dos servicios distintos.
- El precio sugerido sale del catálogo; ➕ solo el personal negocia precio, descuento (≤ subtotal) y vigencia (15 días por defecto).
- `PENDIENTE → EN_PROCESO → APROBADA` (**solo ADMIN**; exige total > 0, vigencia, catálogo activo; genera la cuenta) o `RECHAZADA`.
  ➕ El cliente puede retirar la suya; el ADMIN puede anular una aprobación sin pagos; las vencidas se rechazan solas.
- Una cotización no reserva, no agenda y no inscribe: lo contratado se registra en su módulo.

### Agenda institucional
- Única fuente de la regla **"no podrán existir conflictos de horario"**. Antes de buscar cruces se bloquea la fila del espacio
  (`SELECT … FOR UPDATE`). Rangos pegados (08–10 y 10–12) no chocan.
- ➕ **Eventos** del CFC (graduaciones, conferencias): futuros, 30 min–12 h, con espacio (lo bloquean) o lugar externo.
- Un CLIENTE ve cursos, diplomados y eventos, y solo sus alquileres y catering.

### Pagos
- La **cuenta por cobrar** la crea el sistema al confirmar/aprobar (nadie escribe montos a mano). Monto 0 → nace PAGADA.
- **Abonos**: nunca exceden el saldo (contando lo que está en validación). Transferencia/depósito exigen referencia única.
  - CLIENTE: solo transferencia/depósito → `PENDIENTE_VALIDACION`.
  - RECEPCIONISTA: efectivo/tarjeta quedan confirmados; transferencia/depósito a validación.
  - CONTABILIDAD/ADMIN: validados al registrar; validan, rechazan (motivo) o anulan (error de registro) abonos.
- Estado de la cuenta: `PENDIENTE → PARCIAL → PAGADO` (➕ `ANULADO` si el proceso se cancela sin dinero; se conserva para auditoría).
- **Comprobante** con correlativo `CFC-AAAA-000123` para cada abono validado.
- **Sin reembolsos**: no se cancela un proceso con dinero cobrado o en validación. Nada se finaliza sin estar PAGADO.

### Cierre automático (cada 15 min y en el último abono)
- Inscripción CONFIRMADA + oferta terminada + pagada → FINALIZADA. Reserva / catering confirmados + horario terminado + pagados → COMPLETADA / ATENDIDA.
- ➕ Lo que nadie confirmó a tiempo se cancela solo (inscripción pendiente de una oferta ya terminada; reserva o catering pendiente cuya hora ya llegó) y libera la agenda.
- ➕ Cotizaciones abiertas vencidas → RECHAZADA.

---

## 5. Convenciones de la API

- **Listados**: todos paginados → `{content, page, size, totalElements, totalPages, first, last, sort}`.
  `page` desde 0, `size` 1–100, `sort=campo,asc|desc` (varios con `;`). Los campos ordenables están en una **lista blanca**
  por módulo (ej. `cliente` → `cliente.nombre`); otro campo → 400 con la lista válida.
- **Filtros** como query params (`@ParameterObject`): Swagger los muestra como cajas de texto.
- `DELETE` en catálogos = **desactivar** (borrado lógico) + `PATCH /{id}/activar`. En procesos = **cancelar** (`?motivo=`).
- Errores siempre con `ErrorResponse {timestamp, status, error, code, message, path, details}`:

| code | HTTP | Cuándo |
|---|---|---|
| VALIDATION_ERROR / MALFORMED_REQUEST / INVALID_PARAMETER | 400 | DTO inválido, JSON roto, campo desconocido, enum o fecha mal escritos |
| BUSINESS_RULE | 400 | regla de negocio (virtual con aula, fecha pasada, capacidad...) |
| NO_TOKEN / TOKEN_* / BAD_CREDENTIALS / ACCOUNT_DISABLED | 401 | autenticación |
| ACCESS_DENIED / FORBIDDEN_OPERATION | 403 | rol insuficiente / registro de otro cliente |
| NOT_FOUND / ROUTE_NOT_FOUND | 404 | no existe |
| DUPLICATE, STATE_CONFLICT, INVALID_TRANSITION, SPACE_OCCUPIED, TEACHER_BUSY, CAPACITY_EXCEEDED, CONCURRENT_MODIFICATION | 409 | choca con el estado actual |
| INTERNAL_ERROR | 500 | inesperado (se registra en el log, no se filtra el detalle) |

- Fechas `AAAA-MM-DD`, fecha-hora `AAAA-MM-DDTHH:mm`, horas `HH:mm`; zona America/El_Salvador (reloj único `Clock`).
- Reglas configurables en `application.properties` (`app.reglas.*`): anticipación de catering, horas para que el cliente cancele, vigencia de cotizaciones.

---

## 6. Pruebas unitarias (JUnit 5 + Mockito)

| Clase | Qué prueba |
|---|---|
| `AgendaServiceTest` | **éxito** con aula libre; **solapamiento** → `EspacioOcupadoException`; catering dentro de reserva no bloquea; exclusión del propio registro |
| `CursoServiceTest` | **aula para curso virtual rechazada**; presencial sin aula; cupo > capacidad; sesiones cruzadas; fecha pasada; **docente ocupado**; una actividad de agenda por sesión |
| `ReservaServiceTest` | costo horas × precio; espacio ocupado; capacidad; duración; dueño del registro (403); sin reembolsos; confirmar genera la cuenta |
| `InscripcionServiceTest` | cupo lleno; curso XOR diplomado; empresa sin participante; duplicado; oferta desactivada; confirmar genera la cuenta; transición inválida |
| `PagoServiceTest` | abono > saldo; cliente sin efectivo; referencia obligatoria; efectivo salda y dispara cierre; parcial; transferencia pendiente; sin reembolsos; monto 0 |
| `ReglasCompartidasTest` | cruces de horario, máquina de estados, paginación con lista blanca |

---

## 7. Frontend

`index.html` (oferta pública), `login.html` (login, registro, recuperación) y `app.html` (aplicación por rol).
JS puro sin frameworks: `config.js` (URL del backend: mismo origen o `http://localhost:8080` con CORS), `api.js` (token + refresh automático), `ui.js` (tablas paginadas, filtros, formularios, modales),
`vistas-*.js` por módulo. El menú y los botones dependen del rol y de `accionesPosibles` que devuelve el backend, pero
**la seguridad real siempre la aplica el backend**.
