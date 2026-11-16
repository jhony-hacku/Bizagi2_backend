# Despliegue con Docker Compose

Levanta la aplicacion completa: PostgreSQL, backend (Spring Boot) y front (Angular servido por nginx).
Solo el front publica un puerto; nginx reenvia `/api` al backend dentro de la red de Docker.

## Probar en tu PC (construye las imagenes desde el codigo)

Requiere tener `Bizagi2_front` clonado junto a este repositorio.

```bash
cd deploy
cp .env.example .env        # y cambia las claves
docker compose -f compose.yml -f compose.build.yml up --build -d
```

Abre http://localhost:8080. Para ver logs: `docker compose logs -f backend`.

Como en local se entra por HTTP, pon `JWT_REFRESH_COOKIE_SECURE=false` en el `.env`; con el valor por defecto
(`true`) el navegador no envia las cookies de sesion y no se puede iniciar sesion.

## En un servidor (usa las imagenes publicadas en GHCR)

```bash
cd deploy
docker compose pull
docker compose up -d
```

`BACKEND_TAG` y `FRONT_TAG` en `.env` definen que version se despliega.

## Apagar

```bash
docker compose down          # conserva los datos
docker compose down -v       # borra tambien la base de datos
```
