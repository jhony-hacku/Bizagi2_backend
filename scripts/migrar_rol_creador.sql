\set ON_ERROR_STOP on

BEGIN;

-- Hibernate genera este CHECK en las instalaciones previas de PostgreSQL.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_acceso_check;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_acceso_check
    CHECK (rol_acceso IN ('CREADOR', 'ADMINISTRADOR', 'EDITOR', 'LECTOR'));

-- El registro original guardaba primero la cuenta fundadora y HU-02 solo desactiva.
WITH primer_usuario AS (
    SELECT empresa_id, MIN(id) AS id
    FROM usuarios
    GROUP BY empresa_id
    HAVING COUNT(*) FILTER (WHERE rol_acceso = 'CREADOR') = 0
), promovidos AS (
    UPDATE usuarios u
    SET rol_acceso = 'CREADOR', version_sesion = u.version_sesion + 1, version = u.version + 1
    FROM primer_usuario p
    WHERE u.id = p.id
    RETURNING u.id
)
DELETE FROM refresh_tokens r
USING promovidos p
WHERE r.usuario_id = p.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_creador_por_empresa
    ON usuarios (empresa_id) WHERE rol_acceso = 'CREADOR';

COMMIT;
