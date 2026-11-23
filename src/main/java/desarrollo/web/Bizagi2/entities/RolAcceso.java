package desarrollo.web.Bizagi2.entities;

public enum RolAcceso {
    CREADOR,
    ADMINISTRADOR,
    EDITOR,
    LECTOR;

    public boolean puedeAdministrar() {
        return this == CREADOR || this == ADMINISTRADOR;
    }
}
