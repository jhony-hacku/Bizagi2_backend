# Bizagi2 Backend

Sistema de gestion de procesos multiempresa (visor y editor de procesos). Spring Boot 4.1.1, Java 25, JPA/Hibernate, PostgreSQL y autenticacion con JWT.

Cada empresa se registra con su administrador, crea colaboradores con distintos roles y modela sus procesos como un diagrama (pools, lanes, actividades, gateways, arcos, mensajes y correlaciones). La informacion de una empresa nunca es visible para otra.

## Estructura

```text
src/main/java/desarrollo/web/Bizagi2/
  entities/     Entidades JPA y enums (el modelo de la base de datos)
  repository/   Repositorios Spring Data JPA (uno por tabla)
  service/      Reglas de negocio y transacciones (uno por entidad)
  controller/   Endpoints REST: solo interpretan HTTP y llaman al servicio
  exception/    Excepciones de dominio y GlobalExceptionHandler
  security/     JWT, filtro de autenticacion y reglas por rol
```

Flujo de una peticion: `controller -> service -> repository -> base de datos`. El controlador no toca el repositorio; el servicio aplica las reglas y resuelve las asociaciones.

## Configuracion

Las credenciales van en un archivo `.env` en la raiz del proyecto (esta en `.gitignore`, no se sube al repo). Copia la plantilla y ajusta los valores:

```bash
cp .env.example .env
```

```properties
DB_USERNAME=postgres
DB_PASSWORD=tu_password_de_postgres
JWT_SECRET=<salida de openssl rand -base64 48>
```

La duracion de los tokens y la cookie del refresh token se configuran con `jwt.*` (ver [Autenticacion](#autenticacion-access-token-y-refresh-token)). En local por `http` la cookie Secure solo funciona en `localhost`; si tu navegador no la guarda, usa el perfil `dev`.

Si falta el `.env`, la app no arranca y el error indica la variable que no se pudo resolver (por ejemplo `Could not resolve placeholder 'JWT_SECRET'`).

## Ejecutar

Con PostgreSQL (crear la base una sola vez):

```sql
CREATE DATABASE bizagi2;
```

```bash
./mvnw spring-boot:run
```

Hibernate crea las tablas automaticamente (`ddl-auto=update`).

Sin PostgreSQL, con H2 en memoria (no necesita `.env`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

La consola H2 queda en `http://localhost:8080/h2-console` con JDBC URL `jdbc:h2:mem:bizagi2`, usuario `sa` y contrasena vacia.

Documentacion interactiva de la API: `http://localhost:8080/swagger-ui.html`. Para probar rutas protegidas: hacer login, copiar el `token`, pulsar **Authorize** y pegar `Bearer <token>`. El access token dura 15 minutos; cuando venza, `POST /api/auth/refresh` (el navegador envia la cookie y X-XSRF-TOKEN) entrega uno nuevo.

### Ejecutar con Docker

Docker Compose levanta Spring Boot y PostgreSQL conectados, y conserva los datos de PostgreSQL en un volumen:

```bash
docker compose up --build -d
```

La API queda disponible en `http://localhost:8080` y Swagger en `http://localhost:8080/swagger-ui.html`. PostgreSQL queda publicado en `localhost:5432`.

Para detener los servicios:

```bash
docker compose down
```

Si el puerto `8080` ya esta ocupado, ejecuta la aplicacion en otro puerto:

```powershell
$env:APP_PORT="8081"
docker compose up --build -d
```

## Roles de acceso

| Rol | Puede |
|---|---|
| ADMINISTRADOR | Todo, incluida la gestion de usuarios y los datos de la empresa |
| EDITOR | Consultar y modificar procesos y todo su diagrama |
| LECTOR | Solo consultar |

## Endpoints

Todos requieren el header `Authorization: Bearer <access token>`, salvo `/api/auth/register`, `/api/auth/login` y `/api/auth/refresh` (refresh y logout admiten la cookie del refresh token; GET /api/auth/csrf tambien es publico).

| Recurso | Rutas |
|---|---|
| Autenticacion | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout` |
| Empresa | `GET /api/empresa`, `PUT /api/empresa` |
| Usuarios (solo ADMINISTRADOR, salvo `/me`) | `GET /api/usuarios/me`, `GET/POST /api/usuarios`, `GET/PUT/DELETE /api/usuarios/{id}` (DELETE desactiva), `PUT /api/usuarios/{id}/activar` |
| Procesos | `GET /api/procesos?nombre=&estado=&categoria=&activo=&page=&size=`, `POST /api/procesos`, `GET/PUT/DELETE /api/procesos/{id}`, `GET /api/procesos/{id}/diagrama`, `GET /api/procesos/{id}/historial`, `GET /api/procesos/{id}/validacion` |
| Compartir procesos (HU-23) | `GET/POST /api/procesos/{id}/compartir` (POST solo ADMINISTRADOR, body `{"nit": "..."}`), `DELETE /api/procesos/{id}/compartir/{empresaId}`, `GET /api/procesos/compartidos` |
| Roles de proceso (de la empresa) | `GET /api/roles-proceso?nombre=&page=&size=`, `POST /api/roles-proceso` (solo ADMINISTRADOR), `GET/PUT/DELETE /api/roles-proceso/{id}`, `GET /api/roles-proceso/{id}/historial` |
| Pools | `GET/POST /api/procesos/{procesoId}/pools`, `GET/PUT/DELETE /api/pools/{id}` |
| Lanes | `GET/POST /api/pools/{poolId}/lanes`, `PUT /api/pools/{poolId}/lanes/orden` (body: lista de ids), `GET /api/pools/{poolId}/roles-disponibles`, `GET/PUT/DELETE /api/lanes/{id}` |
| Actividades | `GET /api/pools/{poolId}/actividades`, `POST /api/lanes/{laneId}/actividades`, `GET/PUT/DELETE /api/actividades/{id}` |
| Gateways | `GET/POST /api/pools/{poolId}/gateways`, `GET/PUT/DELETE /api/gateways/{id}` |
| Eventos | `GET/POST /api/pools/{poolId}/eventos`, `GET/PUT/DELETE /api/eventos/{id}` |
| Arcos | `GET/POST /api/pools/{poolId}/arcos`, `GET/PUT/DELETE /api/arcos/{id}` |
| Mensajes | `GET/POST /api/procesos/{procesoId}/mensajes`, `GET/PUT/DELETE /api/mensajes/{id}` |
| Correlacion | `GET/POST/PUT/DELETE /api/mensajes/{mensajeId}/correlacion` |

Las eliminaciones de actividades, gateways, eventos y arcos responden `200` con
`{ arcosEliminados, mensajesEliminados, advertencias }` (las advertencias son las de `/validacion`).

### Registro y login

```json
POST /api/auth/register
{ "nombreEmpresa": "Acme SAS", "nit": "900123456", "emailContacto": "contacto@acme.com",
  "nombre": "Ana Perez", "email": "ana@acme.com", "password": "Clave12345" }

POST /api/auth/login
{ "email": "ana@acme.com", "password": "Clave12345" }
```

Ambos responden con el access token (`token`, `tokenType`, `expiresIn` en segundos) y los datos del usuario, y dejan el refresh token en la cookie `refresh_token` (HttpOnly, nunca en el JSON). El registro crea la empresa y su administrador inicial (todos los campos son obligatorios y el NIT es unico); los demas usuarios los crea el administrador con `POST /api/usuarios` indicando su correo, su contrasena inicial y su rol.

```json
{ "token": "eyJhbGciOi...", "tokenType": "Bearer", "expiresIn": 900,
  "usuarioId": 1, "nombre": "Ana Perez", "email": "ana@acme.com",
  "rolAcceso": "ADMINISTRADOR", "empresaId": 1, "empresa": "Acme SAS" }
```

`POST /api/auth/refresh` (sin cuerpo, con la cookie) responde igual que el login: access token nuevo y refresh token nuevo en la cookie. Si la cookie falta, vencio o ya fue usada responde `401` y borra la cookie: hay que volver a hacer login.

`POST /api/auth/logout` (con el access token en el header) cierra la sesion: el access token deja de servir, se borran los refresh tokens del usuario y se limpia la cookie. Tambien se cierran las sesiones del usuario en otros dispositivos.

### Ejemplos de cuerpos

```json
POST /api/procesos                    { "nombre": "Aprobacion de credito", "descripcion": "...", "categoria": "Finanzas" }
POST /api/roles-proceso               { "nombre": "Analista", "descripcion": "Analiza solicitudes" }
POST /api/procesos/1/pools            { "nombre": "Cliente", "tipoParticipante": "CLIENTE" }
POST /api/pools/1/lanes               { "rolProceso": { "id": 1 } }            (nombre opcional: por defecto el del rol)
POST /api/lanes/1/actividades         { "nombre": "Revisar solicitud", "tipo": "USUARIO", "posicionX": 10, "posicionY": 20 }
PUT  /api/actividades/1               { "nombre": "Revisar", "tipo": "MANUAL", "lane": { "id": 2 } }
POST /api/pools/1/gateways            { "nombre": "Aprobado?", "tipoGateway": "EXCLUSIVA" }
PUT  /api/gateways/1                  { "nombre": "Aprobado?", "tipoGateway": "PARALELA" }     (el PUT lleva el cuerpo completo)
POST /api/pools/1/eventos             { "nombre": "Inicio", "tipoEvento": "INICIO" }
POST /api/pools/1/arcos               { "origen": { "id": 1 }, "destino": { "id": 2 }, "etiqueta": "si", "condicion": "monto <= 1000000" }
POST /api/procesos/1/mensajes         { "nombre": "Aviso", "origen": { "id": 3 }, "destinoPool": { "id": 4 }, "tipoDestino": "CORREO" }
POST /api/mensajes/1/correlacion      { "criterio": "numero de radicado", "accionSinCaso": "DESCARTAR" }
PUT  /api/empresa                     { "nombre": "Acme", "nit": "900111", "emailContacto": "c@acme.com", "editorModificaEstructura": false }
POST /api/procesos/1/compartir        { "nit": "800222" }
```

Valores validos: `tipoParticipante` = `EMPRESA_PROPIETARIA | CLIENTE | PROVEEDOR | SISTEMA_EXTERNO`; `tipoActividad` (campo `tipo`) = `TAREA | USUARIO | MANUAL | SERVICIO | ENVIO`; `tipoGateway` = `EXCLUSIVA | PARALELA | INCLUSIVA`; `tipoEvento` = `INICIO | FIN | MENSAJE_LANZAMIENTO | MENSAJE_RECEPCION_INICIO | MENSAJE_RECEPCION_INTERMEDIO`; `tipoDestino` = `CORREO | SERVICIO_WEB | COLA`; `accionFallo` = `CONTINUAR_FLUJO | DERIVAR_A_MANEJO_ERROR | FINALIZAR_PROCESO`; `accionSinCaso` = `DESCARTAR | INICIAR_CASO_NUEVO`; `estado` = `BORRADOR | PUBLICADO`; `rolAcceso` = `ADMINISTRADOR | EDITOR | LECTOR`.

Obligatorios: actividad (`nombre`, `tipo`), gateway (`nombre`, `tipoGateway`), evento (`nombre`, `tipoEvento`), mensaje (`nombre`, `origen.id`, `destinoPool.id`, y `tipoDestino` si el destino es un `SISTEMA_EXTERNO`), correlacion (`criterio`, `accionSinCaso`), lane (`rolProceso.id`). Los `PUT` llevan el cuerpo completo, no solo el campo que cambia.

Respuestas: los `DELETE` responden `204` sin cuerpo, salvo actividades, gateways, eventos y arcos, que responden `200` con `{ arcosEliminados, mensajesEliminados, advertencias }`. Para comprobar un borrado logico se consulta el recurso y se ve `activo: false` (procesos, usuarios) o `404` (elementos del diagrama y roles). `GET /api/procesos/{id}/validacion` devuelve una lista de `{ nivel, elemento, elementoId, mensaje }` con `nivel` = `ERROR | ADVERTENCIA`.

## Autenticacion: access token y refresh token

La sesion usa dos tokens JWT con funciones distintas. Ninguno reemplaza al otro: un claim `tipo` (`access` / `refresh`) hace que el servidor rechace uno cuando se presenta en lugar del otro.

| | Access token (corto) | Refresh token (largo) |
|---|---|---|
| Duracion | 15 minutos (`jwt.access-expiration-ms`) | 7 dias (`jwt.refresh-expiration-ms`) |
| Para que sirve | Autorizar cada peticion a la API | Obtener un access token nuevo sin pedir la contrasena otra vez |
| Cuando se usa | En todas las rutas protegidas | Solo en `POST /api/auth/refresh`, cuando el access vencio (o esta por vencer) |
| Donde viaja | Header `Authorization: Bearer` | Cookie `refresh_token`: HttpOnly, Secure, SameSite=Strict, ruta `/api/auth` |
| Donde lo guarda el cliente | En memoria de la aplicacion (no en localStorage) | En ningun lado: lo administra el navegador y JavaScript no puede leerlo |
| Que contiene | `sub`, `email`, `rol`, `empresaId`, `v`, `tipo`, `exp` | Solo `sub`, `jti`, `fam`, `v`, `tipo`, `exp` |
| Estado en el servidor | Ninguno (se valida la firma); el filtro consulta al usuario en la BD | Tabla `refresh_tokens`: se puede rotar y revocar |

**Por que dos tokens.** Un solo token tiene que elegir entre seguridad y comodidad: si dura poco, el usuario tendria que loguearse cada pocos minutos; si dura mucho, un token robado da acceso por mucho tiempo y no hay forma de retirarlo. El access token se envia en cada peticion (es el que mas se expone), por eso dura poco: si se filtra, sirve minutos. El refresh token casi no se envia (una vez cada 15 minutos y solo a una ruta), por eso puede durar dias, y como se guarda en la BD se puede revocar.

**Flujo.**

```text
login ──► access (15 min, en el JSON) + refresh (7 dias, en cookie HttpOnly)
peticiones ──► Authorization: Bearer <access>
access vencido ──► 401 ──► POST /api/auth/refresh (cookie) ──► access nuevo + refresh nuevo ──► reintentar
refresh vencido, usado o revocado ──► 401 ──► volver a hacer login
logout ──► se revocan todos los refresh del usuario y se borra la cookie
```

**Practicas aplicadas.**

- **Rotacion de un solo uso:** cada vez que se usa un refresh token se revoca y se entrega otro. La revocacion es un `UPDATE ... WHERE revocado = false` atomico, asi que dos peticiones simultaneas no pueden usar el mismo token.
- **Deteccion de robo:** todos los refresh tokens de un mismo login forman una *familia*. Si llega uno ya usado (alguien lo copio y lo uso despues, o antes que el dueno), se cierran todas las sesiones de la cuenta y el usuario debe iniciar sesion. Esa revocacion se conserva aunque la peticion falle (`noRollbackFor`).
- **Refresh token fuera del alcance de JavaScript:** cookie HttpOnly (un XSS no lo puede leer), Secure (solo HTTPS), SameSite=Strict (no se envia desde otros sitios, defensa CSRF) y limitada a `/api/auth` (no viaja en las demas peticiones). El access token en cambio va en el header, por lo que el resto de la API no es vulnerable a CSRF.
- **Separacion de tipos:** el filtro solo acepta `tipo=access` y `/refresh` solo acepta `tipo=refresh`. Ademas se valida firma (HMAC-SHA), expiracion y emisor (`iss`).
- **Datos minimos en el refresh token:** no lleva rol ni email; el rol y la empresa que se aplican en cada peticion salen siempre de la BD, asi un cambio de rol o una desactivacion tienen efecto de inmediato y no esperan a que venza el token.
- **Revocacion centralizada:** `versionSesion` (claim `v`) viaja en ambos tokens. Al cerrar sesion, cambiar la contraseña, detectar reutilizacion o desactivar al usuario se incrementa y todos sus tokens dejan de servir; ademas el logout borra sus registros de `refresh_tokens`.
- **Solo el `jti` en la BD:** la tabla guarda el id del token, no el token; quien lea la tabla no puede fabricar un refresh token porque no tiene la clave de firma.
- **Respuestas con `Cache-Control: no-store`** en login, registro y refresh, y limpieza diaria (3:00) de los refresh tokens vencidos.
- **Validacion de configuracion al arrancar:** el secreto debe tener al menos 32 caracteres y el refresh debe durar mas que el access.

**Configuracion** (variables de entorno del perfil `prod`; los valores por defecto sirven para empezar):

| Variable | Defecto | Significado |
|---|---|---|
| `JWT_SECRET` | (obligatoria) | Clave de firma, minimo 32 caracteres |
| `JWT_ACCESS_EXPIRATION_MS` | `900000` (15 min) | Duracion del access token |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 dias) | Duracion del refresh token y de la cookie |
| `JWT_REFRESH_COOKIE_SECURE` | `true` | Poner `false` solo para probar por `http` (el perfil `dev` y el `docker-compose.yml` ya lo hacen) |

**Para el frontend.** El frontend incluido conserva el access en memoria, obtiene CSRF, recupera la sesion al recargar y renueva una vez ante 401. Sirve frontend y API bajo el mismo origen con /api por proxy; asi las cookies se envian automaticamente. No habilites CORS con credenciales para origenes arbitrarios.

## Reglas de negocio (en los servicios)

- Multitenancy: todo se consulta por la empresa del token; un recurso de otra empresa responde `404`.
- Borrado logico en todo el diagrama (procesos, pools, lanes, actividades, gateways, eventos, arcos, mensajes, roles): el registro pasa a `activo = false` y deja de verse en las consultas normales.
- Eliminar (salvo pools y lanes): solo ADMINISTRADOR. Pools y lanes: los elimina quien tenga permiso de estructura (HU-24).
- Historial general: cada cambio queda con usuario, fecha, accion, entidad y detalle. `GET /api/procesos/{id}/historial` trae los del proceso y su diagrama; `GET /api/roles-proceso/{id}/historial` los del rol.
- Roles de proceso: son de la empresa (no de un proceso), nombre unico por empresa, solo el ADMINISTRADOR los crea. No se elimina uno que alguna lane use (el `409` dice en que procesos).
- Lane: pertenece a un pool y tiene un rol de proceso. La actividad no tiene rol propio: su responsable es el de su lane. No se elimina una lane con actividades.
- Permiso de estructura (HU-24): `PUT /api/empresa` con `editorModificaEstructura` decide si el EDITOR puede crear, editar y eliminar pools y lanes. El ADMINISTRADOR siempre puede y el LECTOR nunca.
- Pool de participante externo (CLIENTE, PROVEEDOR, SISTEMA_EXTERNO): caja negra, no admite lanes ni elementos. Un pool solo se elimina si esta vacio y sin mensajes dirigidos a el.
- Actividad: requiere `nombre` y `tipo`; el nombre es unico dentro del proceso.
- Arcos: unen elementos del mismo pool (entre pools se usa un mensaje), sin duplicados ni bucles. La `condicion` solo existe si el arco sale de un gateway exclusivo o inclusivo; al pasar un gateway a PARALELA se borran las condiciones de sus arcos salientes. Un Message Catch de inicio no admite arcos entrantes.
- Mensajes: salen de un evento `MENSAJE_LANZAMIENTO` o una actividad `ENVIO` hacia otro pool. Si el destino es un `SISTEMA_EXTERNO` requieren `tipoDestino`. La clave de correlacion (`criterio` y `accionSinCaso`) se define por mensaje.
- Validacion (`GET /api/procesos/{id}/validacion`): devuelve `ERROR` y `ADVERTENCIA`. Los errores (gateway sin dos salientes, arco de gateway sin condicion, Catch de inicio con arcos entrantes) impiden pasar el proceso a `PUBLICADO`; en `BORRADOR` se puede trabajar incompleto. Las advertencias (elementos desconectados, mensaje sin receptor, clave de correlacion ausente o distinta, mensajes ambiguos) solo avisan.
- Compartir (HU-23): solo el ADMINISTRADOR, por NIT de la empresa invitada, en solo lectura. La invitada ve el proceso y su diagrama sin los roles de la propietaria y no ve el historial.
- Un proceso eliminado se puede consultar pero no admite cambios (`409`).

## Formato de errores

```json
{ "status": 404, "message": "Proceso no encontrado", "timestamp": "2026-09-28T03:10:35Z" }
```

`400` datos invalidos o regla de negocio, `401` sin token o credenciales invalidas, `403` sin permiso para la accion, `404` no existe (o es de otra empresa), `409` duplicado o elemento en uso.

## Si ya tenias la base de datos creada con una version anterior

El modelo cambio bastante (roles de proceso por empresa, lane con rol, eventos, mensajes hacia un pool, historial general), y `ddl-auto=update` no elimina ni cambia columnas viejas. Lo mas simple en desarrollo es recrear la base:

```sql
DROP DATABASE bizagi2;
CREATE DATABASE bizagi2;
```

La tabla `historial_procesos` de la version anterior ya no se usa (ahora es `historial`).

Esta version agrega la tabla `refresh_tokens`. Con `ddl-auto=update` se crea sola; con el perfil `prod` (`validate`) hay que crearla antes de desplegar o arrancar una vez con `DDL_AUTO=update`. La propiedad `jwt.expiration-ms` se reemplazo por `jwt.access-expiration-ms` y `jwt.refresh-expiration-ms`.


## Contrato de sesion y pruebas JWT

- Access: 15 minutos, solo en memoria del frontend. Refresh: cookie HttpOnly, SameSite=Strict y Secure salvo el perfil dev.
- Una familia vence siete dias despues del login inicial. Rotar no extiende ese limite.
- Logout cierra todas las sesiones del usuario y es idempotente. Acepta refresh aunque el access este vencido.
- Cambiar contraseña, desactivar un usuario o detectar reutilizacion invalida tambien los access emitidos. Una reutilizacion cierra todas las sesiones de la cuenta.
- Login, registro, refresh y logout requieren CSRF: primero GET `/api/auth/csrf`; despues enviar la cookie `XSRF-TOKEN` tambien como cabecera `X-XSRF-TOKEN`. Angular lo hace automaticamente. No se acepta el token en parametros de formulario.
- La API y el frontend se sirven bajo el mismo origen (`/api` por proxy). No se habilita CORS con credenciales para otros origenes.
- Los clientes HTTP y Swagger deben obtener CSRF y enviar la cabecera antes de POST `/api/auth/*`. La coleccion Postman incluida lo prepara automaticamente.
- Limites locales de login: 10 solicitudes por cuenta y 100 por IP cada cinco minutos. En varias replicas se necesita ademas un limite compartido en el proxy. No se confia en X-Forwarded-For del cliente.
- Docker exige JWT_SECRET sin valor conocido por defecto. Genera la clave con `openssl rand -base64 48`. Para probar compose por HTTP local, declara `JWT_REFRESH_COOKIE_SECURE=false` explicitamente; con HTTPS utiliza true.

Verificacion completa: `./mvnw verify`. `SesionPersistenciaTest` usa repositorios y transacciones reales y cubre rotaciones concurrentes, replay, logout, contraseña y actualizaciones del usuario. Por defecto usa H2; para PostgreSQL usa una base **exclusiva de pruebas**, porque el perfil test crea y elimina su esquema:

```bash
./mvnw -Dtest=SesionPersistenciaTest -Dspring.datasource.url=jdbc:postgresql://localhost:5432/jwt_tests -Dspring.datasource.driver-class-name=org.postgresql.Driver -Dspring.datasource.username=jwt_tests -Dspring.datasource.password=tu_clave test
```

Smoke HTTP sin dependencias Python adicionales (crea cuentas de prueba):

```bash
python scripts/jwt_smoke.py --base-url http://localhost:8080/api
```

Para incluir expiracion real, inicia una instancia local aislada con `--jwt.access-expiration-ms=2000` y ejecuta el smoke con `--expiracion-corta`.
