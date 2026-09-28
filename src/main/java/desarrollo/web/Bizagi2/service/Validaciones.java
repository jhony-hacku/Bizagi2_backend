package desarrollo.web.Bizagi2.service;

import desarrollo.web.Bizagi2.exception.ReglaNegocioException;

// Validaciones simples y reutilizables por todos los servicios
public final class Validaciones {

    private Validaciones() {
    }

    public static void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El campo '" + campo + "' es obligatorio");
        }
    }

    public static void requerido(Object valor, String campo) {
        if (valor == null) {
            throw new ReglaNegocioException("El campo '" + campo + "' es obligatorio");
        }
    }

    public static void emailValido(String email) {
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ReglaNegocioException("El formato del email no es valido");
        }
    }

    public static void passwordValido(String password) {
        if (password.length() < 8) {
            throw new ReglaNegocioException("La contrasena debe tener al menos 8 caracteres");
        }
    }
}
