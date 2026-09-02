# Bizagi2 Backend — Sistema de Gestion y Modelado de Procesos

Backend desarrollado en **Java (Spring Boot)** aplicando los principios de **Clean Architecture / Arquitectura Hexagonal**, **Spring Security con JWT (JJWT 0.12.6)**, **JPA / Hibernate**, soporte para **PostgreSQL** y base de datos en memoria **H2**, documentacion interactiva con **Swagger / OpenAPI 3**, y suite completa de pruebas unitarias y de integracion.

---

## 1. Arquitectura del Proyecto

El codigo esta estructurado siguiendo una **Arquitectura Limpia / Hexagonal**, separando las responsabilidades en capas desacopladas donde el dominio no depende de ningun framework ni tecnologia de persistencia.

```text
src/
├── main/
│   ├── java/desarrollo/web/Bizagi2/
│   │   ├── Bizagi2Application.java
│   │   │
│   │   ├── domain/                              # Dominio puro (independiente de frameworks)
│   │   │   ├── model/
│   │   │   │   ├── User.java                    # Modelo de dominio de usuario
│   │   │   │   └── UserRole.java                # Enum: USER, ADMIN
│   │   │   └── repository/
│   │   │       └── UserRepository.java          # Interfaz de persistencia del dominio
│   │   │
│   │   ├── application/                         # Casos de uso, DTOs y excepciones
│   │   │   ├── dto/
│   │   │   │   ├── LoginRequest.java            # DTO de login con validaciones
│   │   │   │   ├── LoginResponse.java           # DTO con token JWT y datos de sesion
│   │   │   │   ├── RegisterRequest.java         # DTO de registro con validaciones
│   │   │   │   └── RegisterResponse.java        # DTO de respuesta segura sin hash
│   │   │   ├── exception/
│   │   │   │   ├── EmailAlreadyExistsException.java  (409 Conflict)
│   │   │   │   └── InvalidCredentialsException.java  (401 Unauthorized)
│   │   │   └── usecase/
│   │   │       ├── LoginUseCase.java            # Logica de validacion de credenciales y JWT
│   │   │       └── RegisterUserUseCase.java     # Logica de hashing BCrypt y registro
│   │   │
│   │   ├── infrastructure/                      # Adaptadores tecnologicos y frameworks
│   │   │   ├── persistence/
│   │   │   │   ├── UserEntity.java              # Entidad JPA mapeada a la tabla "users"
│   │   │   │   ├── SpringDataUserJpaRepository.java  # Spring Data JPA
│   │   │   │   └── UserRepositoryImpl.java      # Adaptador Domain <-> JPA Entity
│   │   │   └── security/
│   │   │       ├── JwtService.java              # Generacion, firma y validacion de tokens
│   │   │       ├── JwtAuthenticationFilter.java # Filtro OncePerRequest para Bearer JWT
│   │   │       ├── SecurityConfig.java          # Configuracion de Spring Security stateless
│   │   │       ├── PasswordConfig.java          # BCryptPasswordEncoder
│   │   │       ├── RestAuthenticationEntryPoint.java # Manejador de 401 en formato JSON
│   │   │       └── OpenApiConfig.java           # Esquema global Bearer Auth para Swagger
│   │   │
│   │   ├── presentation/                        # Capa Web / Controladores REST
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java          # POST /api/auth/register y /login
│   │   │   │   └── UserController.java          # GET /api/users/me (protegido por JWT)
│   │   │   └── error/
│   │   │       ├── GlobalExceptionHandler.java  # Manejo centralizado de excepciones
│   │   │       └── ApiErrorResponse.java        # Estructura JSON consistente de error
│   │   │
│   │   ├── entities/                            # Entidades JPA del modelador BPMN (conservadas)
│   │   │   ├── Empresa.java
│   │   │   ├── Usuario.java                     # Usuario de negocio BPMN (tabla "usuarios")
│   │   │   ├── Proceso.java
│   │   │   ├── Pool.java
│   │   │   ├── Lane.java
│   │   │   ├── NodoFlujo.java                  # Herencia SINGLE_TABLE
│   │   │   ├── Actividad.java                  # extends NodoFlujo
│   │   │   ├── Gateway.java                    # extends NodoFlujo
│   │   │   ├── Arco.java
│   │   │   ├── Mensaje.java
│   │   │   ├── Correlacion.java
│   │   │   ├── RolProceso.java
│   │   │   ├── RolAcceso.java
│   │   │   ├── EstadoProceso.java
│   │   │   ├── TipoGateway.java
│   │   │   └── TipoParticipante.java
│   │   │
│   │   └── repository/                         # Repositorios JPA de procesos BPMN
│   │       ├── UsuarioRepository.java
│   │       ├── ProcesoRepository.java
│   │       ├── ArcoRepository.java
│   │       ├── CorrelacionRepository.java
│   │       ├── EmpresaRepository.java
│   │       ├── LaneRepository.java
│   │       ├── MensajeRepository.java
│   │       ├── NodoFlujoRepository.java
│   │       ├── PoolRepository.java
│   │       └── RolProcesoRepository.java
│   │
│   └── resources/
│       ├── application.properties               # Configuracion PostgreSQL / Produccion
│       └── application-dev.properties           # Perfil local con H2 y consola web
│
└── test/
    ├── java/desarrollo/web/Bizagi2/
    │   ├── Bizagi2ApplicationTests.java
    │   ├── application/usecase/
    │   │   ├── LoginUseCaseTest.java            # Pruebas unitarias de Login
    │   │   └── RegisterUserUseCaseTest.java     # Pruebas unitarias de Registro
    │   └── presentation/controller/
    │       └── AuthIntegrationTest.java         # 10 pruebas de integracion completas
    └── resources/
        └── application-test.properties          # Configuracion H2 para ejecucion de tests
```

---

## 2. Modelo de Base de Datos y Coexistencia de Entidades

El proyecto mantiene una separacion clara entre el **Modulo de Autenticacion** y el **Modulo de Procesos/BPMN**:

```text
               ┌───────────────────────────────────────────────────────────┐
               │              MODULO DE AUTENTICACION / JWT                │
               │                                                           │
               │   UserEntity (tabla: users)                               │
               │     ├── id (BIGINT PK)                                    │
               │     ├── username (VARCHAR)                                │
               │     ├── email (VARCHAR UNIQUE)                            │
               │     ├── password_hash (VARCHAR - BCrypt)                  │
               │     ├── role (VARCHAR - USER / ADMIN)                     │
               │     └── created_at (TIMESTAMP)                            │
               └───────────────────────────────────────────────────────────┘

               ┌───────────────────────────────────────────────────────────┐
               │                MODULO DE MODELADO BPMN                    │
               │                                                           │
               │   Empresa                                                 │
               │     ├── Usuario (tabla: usuarios, RolAcceso)              │
               │     └── Proceso (tabla: procesos)                         │
               │           └── Pool (tabla: pools)                         │
               │                 ├── Lane (tabla: lanes)                   │
               │                 ├── NodoFlujo (tabla: nodos_flujo)        │
               │                 │     ├── Actividad (tipo: ACTIVIDAD)     │
               │                 │     └── Gateway (tipo: GATEWAY)         │
               │                 ├── Arco (tabla: arcos)                   │
               │                 └── Mensaje (tabla: mensajes)             │
               │                       └── Correlacion                     │
               └───────────────────────────────────────────────────────────┘
```

> **Nota:** `UserEntity` (tabla `users`) se encarga de la seguridad y el JWT, mientras que `Usuario` (tabla `usuarios`) pertenece al contexto de modelado de procesos organizacionales. Ambas coexisten sin conflictos.

---

## 3. Flujo de Seguridad y Autenticacion

1. **Registro:**
   - La contraseña es encriptada usando `BCryptPasswordEncoder` (con salt aleatorio por cada contraseña).
   - Se valida formato de email y longitud minima de contraseña (minimo 8 caracteres).
   - Si el email ya esta registrado, retorna `409 Conflict`.
   - **Nunca** se almacena ni se expone la contraseña o el hash en las respuestas.

2. **Login y Generacion de JWT:**
   - Se validan las credenciales con `PasswordEncoder.matches()`.
   - En caso de error, retorna un generico `401 Unauthorized` (evita enumeracion de usuarios).
   - Si es exitoso, `JwtService` genera un token firmado con algoritmo **HMAC-SHA256** a partir de una clave secreta (`JWT_SECRET`).
   - El token contiene claims: `sub` (userId), `email`, `username`, `role`, `iat`, `exp`.

3. **Filtro de Seguridad (`JwtAuthenticationFilter`):**
   - Intercepta cada peticion entrante buscando el encabezado `Authorization: Bearer <token>`.
   - Valida la firma y la fecha de expiracion del token.
   - Establece la identidad y los roles (`ROLE_USER`, `ROLE_ADMIN`) en el `SecurityContextHolder`.

---

## 4. Perfiles de Configuracion

El proyecto cuenta con 3 perfiles preparados segun la necesidad:

| Perfil | Base de Datos | Archivo | Uso Principal |
|---|---|---|---|
| **Default** | **PostgreSQL** | `application.properties` | Entorno estandar / produccion con PostgreSQL en `localhost:5432/bizagi2`. |
| **`dev`** | **H2 en Memoria** | `application-dev.properties` | Desarrollo local sin necesidad de instalar PostgreSQL. Incluye consola H2 en `/h2-console`. |
| **`test`** | **H2 en Memoria** | `application-test.properties` | Ejecucion de pruebas automatizadas aisladas (`mvn test`). |

---

## 5. Base de Datos H2 en Memoria (Perfil `dev`)

Cuando aun **no tengas PostgreSQL instalado o corriendo**, utiliza el perfil **`dev`** para ejecutar la aplicacion completamente funcional en memoria:

### Credenciales y Conexion H2 por Defecto:

| Parametro | Valor |
|---|---|
| **Consola Web H2** | http://localhost:8080/h2-console |
| **Driver Class** | `org.h2.Driver` |
| **JDBC URL** | `jdbc:h2:mem:bizagi2` |
| **User Name** | `sa` |
| **Password** | *(dejar vacio / sin contraseña)* |

> **Nota:** Al entrar a `http://localhost:8080/h2-console`, asegurate de que el campo **JDBC URL** sea exactamente `jdbc:h2:mem:bizagi2`, deja la contraseña vacia y haz clic en **Connect** para inspeccionar las tablas (`users`, `procesos`, `nodos_flujo`, etc.) y hacer consultas SQL directamente.

---

## 6. Guia de Ejecucion

### Requisitos Previos
- **Java:** JDK 21 o superior (ej. JDK 25).
- **Maven:** Incluido en el proyecto mediante el wrapper `./mvnw` / `mvnw.cmd`.

---

### A. Ejecutar en IntelliJ IDEA con H2 (Perfil `dev`)

Para que la aplicacion corra con **H2** directamente desde **IntelliJ IDEA**:

1. En el menu superior de IntelliJ, ve a **Run** -> **Edit Configurations...** (o haz clic en el desplegable junto al boton de Play).
2. Selecciona tu configuracion de **`Bizagi2Application`** (bajo *Spring Boot* o *Application*).
3. Configura el perfil `dev` de **cualquiera de estas formas**:
   - **Forma 1 (Active profiles):** Si ves el campo *Active profiles*, escribe: `dev`
   - **Forma 2 (Program arguments):** Escribe: `--spring.profiles.active=dev`
   - **Forma 3 (VM options):** Escribe: `-Dspring.profiles.active=dev`
4. Haz clic en **Apply** y luego en **OK**.
5. Presiona **Run (Play)**.

La aplicacion levantara inmediatamente en `http://localhost:8080` usando H2, sin requerir PostgreSQL.

---

### B. Ejecutar por Terminal con H2 (Perfil `dev`)

```powershell
# En PowerShell (Windows)
$env:JAVA_HOME = "C:\Users\Jonatan\.jdks\openjdk-25"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### C. Ejecutar las Pruebas Automatizadas (16 tests)
Todas las pruebas se ejecutan automaticamente en memoria con H2:

```powershell
.\mvnw.cmd test
```

Resultado:
```text
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### D. Iniciar con PostgreSQL (Perfil Default)

Cuando ya tengas PostgreSQL instalado y creado la base de datos:
```sql
CREATE DATABASE bizagi2;
```

Variables de entorno configurables (o toma los valores por defecto):
```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/bizagi2
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=javeriana
JWT_SECRET=tu-clave-secreta-de-al-menos-32-caracteres
JWT_EXPIRATION=3600000
```

Ejecutas normalmente sin ningun perfil adicional:
```powershell
.\mvnw.cmd spring-boot:run
```

---

## 7. Documentacion Swagger / OpenAPI 3

Con la aplicacion iniciada, accede a Swagger UI en el navegador:

- **http://localhost:8080/swagger-ui.html**
- **http://localhost:8080/swagger-ui/index.html**

### Como autenticarse en Swagger UI:
1. Registra o haz login en `/api/auth/login` y copia el campo `token`.
2. Haz clic en el boton **Authorize** (arriba a la derecha).
3. En el campo `bearerAuth`, escribe: `Bearer <tu_token_aqui>`.
4. Haz clic en **Authorize** y luego **Close**. Ahora podras probar los endpoints protegidos.

---

## 8. Catalogo de Endpoints de Autenticacion

### 1. Registro de Usuario
- **Ruta:** `POST /api/auth/register`
- **Acceso:** Publico
- **Headers:** `Content-Type: application/json`

**Request Body:**
```json
{
  "username": "jonatan",
  "email": "jonatan@example.com",
  "password": "PasswordSeguro123"
}
```

**Response (201 Created):**
```json
{
  "userId": 1,
  "username": "jonatan",
  "email": "jonatan@example.com",
  "role": "USER"
}
```

---

### 2. Inicio de Sesion (Login)
- **Ruta:** `POST /api/auth/login`
- **Acceso:** Publico
- **Headers:** `Content-Type: application/json`

**Request Body:**
```json
{
  "email": "jonatan@example.com",
  "password": "PasswordSeguro123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJqb25hdGFuQGV4YW1wbGUuY29tIiwidXNlcm5hbWUiOiJqb25hdGFuIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3NDAwMDAwMDAsImV4cCI6MTc0MDAwMzYwMH0...",
  "userId": 1,
  "username": "jonatan",
  "role": "USER"
}
```

---

### 3. Obtener Usuario Autenticado
- **Ruta:** `GET /api/users/me`
- **Acceso:** Requiere Autenticacion (Bearer JWT)
- **Headers:** `Authorization: Bearer <TOKEN>`

**Response (200 OK):**
```json
{
  "principal": "jonatan@example.com",
  "authorities": [
    "ROLE_USER"
  ]
}
```

---

## 9. Formato Estandar de Errores (`GlobalExceptionHandler`)

Todas las respuestas de error siguen una estructura JSON homogenea:

```json
{
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2026-09-01T20:45:00.000Z"
}
```

Codigos gestionados:
- **`400 Bad Request`**: Errores de validacion de campos (`@Valid`).
- **`401 Unauthorized`**: Credenciales invalidas o token inexistente/vencido.
- **`403 Forbidden`**: Sin permisos suficientes para el recurso.
- **`409 Conflict`**: Correo electronico duplicado en registro.
- **`500 Internal Server Error`**: Excepciones no controladas.
