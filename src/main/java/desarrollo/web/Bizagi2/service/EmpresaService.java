package desarrollo.web.Bizagi2.service;

import static desarrollo.web.Bizagi2.service.Validaciones.emailValido;
import static desarrollo.web.Bizagi2.service.Validaciones.requerido;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desarrollo.web.Bizagi2.entities.Empresa;
import desarrollo.web.Bizagi2.exception.ConflictoDominioException;
import desarrollo.web.Bizagi2.exception.RecursoNoEncontradoException;
import desarrollo.web.Bizagi2.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    // HU-01: nombre, NIT (unico) y correo de contacto, todos obligatorios
    public Empresa crear(String nombre, String nit, String emailContacto) {
        requerido(nombre, "nombreEmpresa");
        requerido(nit, "nit");
        requerido(emailContacto, "emailContacto");
        emailValido(emailContacto.trim());
        if (empresaRepository.existsByNit(nit.trim())) {
            throw new ConflictoDominioException("Ya existe una empresa registrada con ese NIT");
        }
        Empresa empresa = new Empresa();
        empresa.setNombre(nombre.trim());
        empresa.setNit(nit.trim());
        empresa.setEmailContacto(emailContacto.trim().toLowerCase());
        return empresaRepository.save(empresa);
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorId(Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
    }

    public Empresa actualizar(Integer id, Empresa datos) {
        requerido(datos.getNombre(), "nombre");
        requerido(datos.getNit(), "nit");
        requerido(datos.getEmailContacto(), "emailContacto");
        emailValido(datos.getEmailContacto().trim());
        Empresa empresa = buscarPorId(id);
        String nit = datos.getNit().trim();
        if (!nit.equals(empresa.getNit()) && empresaRepository.existsByNit(nit)) {
            throw new ConflictoDominioException("Ya existe una empresa registrada con ese NIT");
        }
        empresa.setNombre(datos.getNombre().trim());
        empresa.setNit(nit);
        empresa.setEmailContacto(datos.getEmailContacto().trim().toLowerCase());
        if (datos.getEditorModificaEstructura() != null) {
            empresa.setEditorModificaEstructura(datos.getEditorModificaEstructura());
        }
        return empresaRepository.save(empresa);
    }
}
