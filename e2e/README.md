# Pruebas E2E con Selenium

Recorren la aplicacion desplegada con un Chrome real, como lo haria un usuario:
registro de empresa (HU-01), inicio y cierre de sesion (HU-03) y creacion de procesos (HU-04).

Es un proyecto Maven aparte: no corre con el `mvn verify` del backend.

## Ejecutarlas

Requiere Chrome instalado y JDK 21 o superior. Desde la raiz del repositorio:

```bash
# Contra staging
./mvnw -f e2e/pom.xml test -De2e.baseUrl=https://DIRECCION-DE-STAGING

# Viendo el navegador (por defecto corre sin ventana)
./mvnw -f e2e/pom.xml test -De2e.baseUrl=https://DIRECCION-DE-STAGING -De2e.headless=false

# Contra el front local (ng serve en http://localhost:4200, con el backend en perfil dev)
./mvnw -f e2e/pom.xml test
```

Cada prueba registra una empresa nueva con datos unicos, asi que se pueden repetir sin limpiar la base de datos.
Si una prueba falla, queda una captura de pantalla en `e2e/target/screenshots/`.

La sesion usa cookies seguras: contra un servidor por HTTP hay que tener `JWT_REFRESH_COOKIE_SECURE=false`.
