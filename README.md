# Bizagi2 Backend

Sistema de gestion y modelado de procesos de negocio BPMN 2.0 multiempresa (visor y editor de procesos). Desarrollado con Spring Boot 4.1.1, Java 25, JPA / Hibernate, PostgreSQL 17, seguridad JWT (HMAC-SHA), analisis estatico con SonarCloud y cobertura automatizada con JaCoCo.

Cada empresa se registra de forma independiente con su administrador inicial, gestiona colaboradores con diferentes roles y modela sus diagramas (pools, lanes, actividades, gateways, arcos de secuencia, mensajes y correlaciones). El aislamiento de datos (multitenancy) garantiza que la informacion de una empresa nunca sea visible para otra.

---

## Arquitectura y Estructura

El backend implementa una arquitectura en capas desacoplada (`Controller -> Service -> Repository -> Database`):

```text
src/main/java/desarrollo/web/Bizagi2/
  ├── Bizagi2Application.java   # Clase de arranque Spring Boot
  ├── controller/               # Endpoints REST (HTTP, validacion de entrada y serializacion)
  ├── service/                  # Reglas de negocio BPMN, transacciones y aislamiento multitenant
  ├── repository/               # Repositorios Spring Data JPA
  ├── entities/                 # Entidades JPA, enums y modelo de dominio
  ├── exception/                # Excepciones de dominio y GlobalExceptionHandler (@RestControllerAdvice)
  └── security/                 # Filtro JWT, servicio de tokens y configuracion de seguridad
```

Flujo de peticion: `Controller -> Service -> Repository -> Base de datos`. El controlador nunca interactua directamente con los repositorios; los servicios resuelven las asociaciones, validan permisos y aplican las reglas del modelador BPMN.

---

## Requisitos y Configuracion del Entorno

El proyecto requiere **Java 25** y **Maven 3.9+** (gestionado via Maven Wrapper `./mvnw` o `mvnw.cmd`).

### Opcion A: Linux (Ubuntu / Debian)

```bash
# Dependencias base
sudo apt update && sudo apt install -y curl git postgresql docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $USER

# Instalacion de Java 25 recomendada via SDKMAN:
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-temurin
```

### Opcion B: Linux (Arch Linux)

```bash
# Dependencias base
sudo pacman -Syu
sudo pacman -S jdk-openjdk maven docker postgresql
sudo systemctl enable --now docker
sudo usermod -aG docker $USER

# Para configurar la version activa de Java:
archlinux-java status
sudo archlinux-java set <nombre-mostrado-en-status>   # ej. java-25-openjdk

# O alternativamente via SDKMAN:
# sdk install java 25-temurin
```

### Opcion C: Windows (PowerShell)

1. Descargar e instalar OpenJDK 25 (por ejemplo Eclipse Temurin 25 o Microsoft OpenJDK 25).
2. Configurar la variable de entorno `JAVA_HOME` en PowerShell antes de ejecutar comandos:
   ```powershell
   $env:JAVA_HOME="C:\Ruta\A\Tu\jdk-25"
   ```
3. Ejecutar los comandos utilizando el script wrapper `.\mvnw.cmd`.

---

## Perfiles de Configuracion

El sistema cuenta con configuraciones desacopladas para cada escenario:

| Perfil | Archivo | Proposito |
| :--- | :--- | :--- |
| **dev** | `src/main/resources/application-dev.properties` | Base H2 en memoria para desarrollo rapido local sin PostgreSQL ni `.env`. Consola web en `/h2-console`. |
| **prod** | `src/main/resources/application-prod.yml` | Produccion, Docker y Kubernetes: 100% parametrizado mediante variables de entorno, sin valores fijos. |
| **test** | `src/test/resources/application-test.properties` | Entorno de pruebas unitarias y de integracion automatizadas con H2 en memoria. |
| *(default)* | `src/main/resources/application.properties` | Configuracion local con PostgreSQL que lee credenciales desde `.env`. |

### Variables de Entorno del Perfil `prod`

El perfil `application-prod.yml` lee los siguientes parametros:

```bash
DB_URL=jdbc:postgresql://<host>:<puerto>/<base_datos>
DB_USER=<usuario_postgres>
DB_PASSWORD=<password_postgres>
JWT_SECRET=<clave_secreta_minimo_32_caracteres>
DDL_AUTO=validate            # Por defecto 'validate' por seguridad. Usar 'update' solo en arranque inicial o CI.
JWT_EXPIRATION_MS=86400000   # Opcional, por defecto 24 horas (86400000 ms)
SWAGGER_ENABLED=true         # Opcional, true/false
```

---

## Modos de Ejecucion

Asegurate de que el wrapper tenga permisos de ejecucion en Linux:
```bash
chmod +x ./mvnw
```

### 1. Modo Desarrollo Rapido (Perfil `dev`)
No requiere base de datos externa ni `.env`:

- En Linux:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```
- En Windows (PowerShell):
  ```powershell
  .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
  ```

- Consola H2: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:bizagi2`, Usuario: `sa`, Contrasena vacia).
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 2. Modo Produccion (Perfil `prod`)
Requiere definir las variables de entorno:

- En Linux:
  ```bash
  export DB_URL=jdbc:postgresql://localhost:5432/bizagi2
  export DB_USER=bizagi
  export DB_PASSWORD=test
  export JWT_SECRET=clave-secreta-super-larga-y-segura-de-mas-de-32-bytes
  export DDL_AUTO=update
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
  ```
- En Windows (PowerShell):
  ```powershell
  $env:DB_URL="jdbc:postgresql://localhost:5432/bizagi2"
  $env:DB_USER="bizagi"
  $env:DB_PASSWORD="test"
  $env:JWT_SECRET="clave-secreta-super-larga-y-segura-de-mas-de-32-bytes"
  $env:DDL_AUTO="update"
  .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
  ```

---

## Docker y Smoke Testing

El archivo `Dockerfile` implementa una construccion multi-etapa:
- **Build stage:** `eclipse-temurin:25-jdk` descarga dependencias y empaqueta el JAR con `./mvnw clean package -DskipTests`.
- **Runtime stage:** `eclipse-temurin:25-jre` ejecuta el contenedor bajo un usuario sin privilegios (`appuser`, UID 1001).

### Construir la imagen localmente
```bash
docker build -t bizagi2-backend .
```

### Ejecutar Smoke Test con PostgreSQL 17 (Identico al CI)
Prueba que valida la imagen Docker, la creacion de esquemas en PostgreSQL y el flujo completo de autenticacion:

```bash
# 1. Crear red de prueba
docker network create test-net

# 2. Iniciar PostgreSQL 17
docker run -d --name db --network test-net \
  -e POSTGRES_DB=bizagi2 -e POSTGRES_USER=bizagi -e POSTGRES_PASSWORD=test \
  postgres:17

# 3. Esperar que la base de datos este lista
until docker exec db pg_isready -U bizagi -d bizagi2; do sleep 2; done
sleep 3

# 4. Iniciar contenedor de la aplicacion
docker run -d --name app --network test-net -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://db:5432/bizagi2 \
  -e DB_USER=bizagi \
  -e DB_PASSWORD=test \
  -e DDL_AUTO=update \
  -e JWT_SECRET=ci-secret-ci-secret-ci-secret-ci-secret-1234 \
  bizagi2-backend

# 5. Probar endpoints
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nombreEmpresa":"Acme","nit":"900123456","emailContacto":"info@acme.com","nombre":"Admin","email":"admin@acme.com","password":"Clave12345"}'

# 6. Limpieza
docker rm -f app db
docker network rm test-net
```

---

## Pruebas Unitarias, JaCoCo y SonarCloud

El proyecto cuenta con una suite automatizada de pruebas unitarias que cubre las Historias de Usuario (HU-01 a HU-28), las clases de servicio BPMN, seguridad y manejo de excepciones.

### Ejecutar pruebas y reporte de cobertura
- Linux:
  ```bash
  ./mvnw clean verify
  ```
- Windows:
  ```powershell
  .\mvnw.cmd clean verify
  ```

El reporte HTML se genera en `target/site/jacoco/index.html`.

### Exclusiones de Cobertura en pom.xml
Para asegurar metricas de cobertura precisas, se sincronizaron las exclusiones en `<sonar.coverage.exclusions>` y en el plugin `jacoco-maven-plugin`:
- **Excluidos:** `entities/**`, `dto/**`, `config/**`, `security/*Config.*`, `exception/*Exception.*` (POJOs simples que solo extienden `RuntimeException`) y `Bizagi2Application.*`.
- **Incluido:** `GlobalExceptionHandler.java` (`@RestControllerAdvice` con logica de traduccion de codigos HTTP), cubierto exhaustivamente con pruebas unitarias.

---

## Pipeline CI/CD en GitHub Actions (`.github/workflows/ci.yml`)

El pipeline automatizado unificado se ejecuta en pushes y pull requests hacia `main` y `develop`:

1. **Job `build-test-sonar`:**
   - Compila con JDK 25 Temurin.
   - Ejecuta `mvn -B clean verify` (compilacion, pruebas y reporte JaCoCo).
   - Publica los resultados de pruebas mediante `dorny/test-reporter`.
   - Preserva el reporte de JaCoCo como artefacto descargable.
   - Ejecuta el analisis de SonarCloud exigiendo el cumplimiento del Quality Gate (`sonar.qualitygate.wait=true`).
2. **Job `docker-build`:**
   - Construye la imagen Docker `bizagi2-backend:${{ github.sha }}`.
   - Despliega un contenedor efimero de PostgreSQL 17.
   - Realiza un **Smoke Test E2E** ejecutando registro empresarial (HU-01), login (HU-03) y llamada a endpoints protegidos con JWT con validacion de codigos de respuesta HTTP.

---

## Roles de Acceso

| Rol | Descripcion de Privilegios |
| :--- | :--- |
| ADMINISTRADOR | Gestion total de la empresa, administracion de usuarios, edicion del diagrama y eliminacion de recursos. |
| EDITOR | Creacion y modificacion de procesos, diagramas y elementos. La creacion y eliminacion de estructura (pools y lanes) depende de la configuracion empresarial `editorModificaEstructura` (HU-24). |
| LECTOR | Consulta de diagramas y procesos en modo solo lectura. |

---

## Resumen de Endpoints de la API

Todos los endpoints requieren el header `Authorization: Bearer <token>`, excepto `/api/auth/*`.

| Recurso | Rutas Principales |
| :--- | :--- |
| **Autenticacion** | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout` |
| **Empresa** | `GET /api/empresa`, `PUT /api/empresa` |
| **Usuarios** | `GET /api/usuarios/me`, `GET /api/usuarios`, `POST /api/usuarios`, `PUT /api/usuarios/{id}`, `DELETE /api/usuarios/{id}`, `PUT /api/usuarios/{id}/activar` |
| **Procesos** | `GET/POST /api/procesos`, `GET/PUT/DELETE /api/procesos/{id}`, `GET /api/procesos/{id}/diagrama`, `GET /api/procesos/{id}/historial`, `GET /api/procesos/{id}/validacion` |
| **Compartir (HU-23)** | `GET/POST /api/procesos/{id}/compartir`, `DELETE /api/procesos/{id}/compartir/{empresaId}`, `GET /api/procesos/compartidos` |
| **Roles de Proceso** | `GET/POST /api/roles-proceso`, `GET/PUT/DELETE /api/roles-proceso/{id}`, `GET /api/roles-proceso/{id}/historial` |
| **Pools** | `GET/POST /api/procesos/{procesoId}/pools`, `GET/PUT/DELETE /api/pools/{id}` |
| **Lanes** | `GET/POST /api/pools/{poolId}/lanes`, `PUT /api/pools/{poolId}/lanes/orden`, `GET /api/pools/{poolId}/roles-disponibles`, `GET/PUT/DELETE /api/lanes/{id}` |
| **Actividades** | `GET /api/pools/{poolId}/actividades`, `POST /api/lanes/{laneId}/actividades`, `GET/PUT/DELETE /api/actividades/{id}` |
| **Gateways** | `GET/POST /api/pools/{poolId}/gateways`, `GET/PUT/DELETE /api/gateways/{id}` |
| **Eventos** | `GET/POST /api/pools/{poolId}/eventos`, `GET/PUT/DELETE /api/eventos/{id}` |
| **Arcos** | `GET/POST /api/pools/{poolId}/arcos`, `GET/PUT/DELETE /api/arcos/{id}` |
| **Mensajes** | `GET/POST /api/procesos/{procesoId}/mensajes`, `GET/PUT/DELETE /api/mensajes/{id}` |
| **Correlacion** | `GET/POST/PUT/DELETE /api/mensajes/{mensajeId}/correlacion` |

---

## Reglas de Negocio BPMN

- **Multitenancy Estricto:** Toda peticion se filtra por la empresa del token; cualquier recurso de otra empresa responde `404 Not Found`.
- **Borrado Logico:** Procesos y elementos del diagrama utilizan borrado logico (`activo = false`) para conservar integridad referencial e historial de auditoria.
- **Participantes Externos:** Pools tipo `CLIENTE`, `PROVEEDOR` o `SISTEMA_EXTERNO` se consideran cajas negras (no admiten lanes ni elementos internos).
- **Arcos vs Mensajes:** Los arcos de secuencia solo conectan nodos del mismo pool; la comunicacion inter-pool se realiza unicamente con mensajes.
- **Validacion del Proceso (`/validacion`):**
  - **Errores:** Bloquean el paso a `PUBLICADO` (gateways sin bifurcacion, arcos condicionales sin expresion de condicion, eventos Message Catch iniciales con arcos entrantes).
  - **Advertencias:** Notificaciones sobre elementos huerfanos o mensajes sin receptor, permitidas mientras el proceso este en `BORRADOR`.
