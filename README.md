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
JWT_SECRET=una-clave-secreta-de-al-menos-32-caracteres
```

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

Documentacion interactiva de la API: `http://localhost:8080/swagger-ui.html`. Para probar rutas protegidas: hacer login, copiar el `token`, pulsar **Authorize** y pegar `Bearer <token>`.

## Roles de acceso

| Rol | Puede |
|---|---|
| ADMINISTRADOR | Todo, incluida la gestion de usuarios y los datos de la empresa |
| EDITOR | Consultar y modificar procesos y todo su diagrama |
| LECTOR | Solo consultar |

## Endpoints

Todos requieren el header `Authorization: Bearer <token>`, salvo `/api/auth/*`.

| Recurso | Rutas |
|---|---|
| Autenticacion | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout` |
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

Ambos responden con el `token` y los datos del usuario. El registro crea la empresa y su administrador inicial (todos los campos son obligatorios y el NIT es unico); los demas usuarios los crea el administrador con `POST /api/usuarios` indicando su correo, su contrasena inicial y su rol.

`POST /api/auth/logout` (con el token en el header) cierra la sesion: el token deja de servir y tambien los demas tokens que el usuario tuviera abiertos.

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
