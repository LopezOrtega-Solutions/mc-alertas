package com.terrahome.msalertas.service;

import com.terrahome.msalertas.model.dto.ActualizarAlertaRequest;
import com.terrahome.msalertas.model.dto.AlertaResponse;
import com.terrahome.msalertas.model.dto.CrearAlertaRequest;
import com.terrahome.msalertas.model.dto.ListaAlertasResponse;
import com.terrahome.msalertas.model.entity.Alerta;
import com.terrahome.msalertas.model.entity.EstadoAlerta;
import com.terrahome.msalertas.model.entity.Operacion;
import com.terrahome.msalertas.model.entity.TipoPropiedad;
import com.terrahome.msalertas.repository.AlertaRepository;
import com.terrahome.msalertas.security.NegocioException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio MS-Alertas (US1, US2, US4, US5). El dueño sale del
 * claim sub del JWT, nunca del body (spec FR-003).
 */
@Service
public class AlertaService {

    private final AlertaRepository repository;

    public AlertaService(AlertaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AlertaResponse crear(CrearAlertaRequest req, Long usuarioId) {
        validarNombre(req.getNombre());
        TipoPropiedad tipo = parseTipo(req.getTipo());
        Operacion operacion = parseOperacion(req.getOperacion());
        validarPrecios(req.getPrecioMin(), req.getPrecioMax());
        exigirAlMenosUnCriterio(req.getCiudad(), tipo, operacion, req.getPrecioMin(), req.getPrecioMax());

        Alerta a = new Alerta();
        a.setUsuarioId(usuarioId);
        a.setNombre(req.getNombre().trim());
        a.setCiudad(normalizar(req.getCiudad()));
        a.setTipo(tipo);
        a.setOperacion(operacion);
        a.setPrecioMin(req.getPrecioMin());
        a.setPrecioMax(req.getPrecioMax());
        a.setEstado(EstadoAlerta.ACTIVA);
        return new AlertaResponse(repository.save(a));
    }

    @Transactional(readOnly = true)
    public ListaAlertasResponse listar(Long usuarioId, Long filtroUsuarioId, boolean esAdmin, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long objetivo = usuarioId;
        if (filtroUsuarioId != null) {
            if (!esAdmin) {
                throw NegocioException.permisosInsuficientes();
            }
            objetivo = filtroUsuarioId;
        }
        Page<Alerta> result = repository.findByUsuarioId(objetivo, PageRequest.of(safePage, safeSize));
        List<AlertaResponse> items = result.getContent().stream().map(AlertaResponse::new).toList();
        return new ListaAlertasResponse(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public AlertaResponse obtener(Long id, Long usuarioId, boolean esAdmin) {
        return new AlertaResponse(buscarConPermiso(id, usuarioId, esAdmin));
    }

    @Transactional
    public AlertaResponse actualizar(Long id, ActualizarAlertaRequest req, Long usuarioId, boolean esAdmin) {
        Alerta a = buscarConPermiso(id, usuarioId, esAdmin);

        String nombre = req.getNombre() != null ? req.getNombre() : a.getNombre();
        String ciudad = req.getCiudad() != null ? req.getCiudad() : a.getCiudad();
        TipoPropiedad tipo = req.getTipo() != null ? parseTipo(req.getTipo()) : a.getTipo();
        Operacion operacion = req.getOperacion() != null ? parseOperacion(req.getOperacion()) : a.getOperacion();
        BigDecimal precioMin = req.getPrecioMin() != null ? req.getPrecioMin() : a.getPrecioMin();
        BigDecimal precioMax = req.getPrecioMax() != null ? req.getPrecioMax() : a.getPrecioMax();

        validarNombre(nombre);
        validarPrecios(precioMin, precioMax);
        exigirAlMenosUnCriterio(ciudad, tipo, operacion, precioMin, precioMax);

        a.setNombre(nombre.trim());
        a.setCiudad(normalizar(ciudad));
        a.setTipo(tipo);
        a.setOperacion(operacion);
        a.setPrecioMin(precioMin);
        a.setPrecioMax(precioMax);

        if (req.getEstado() != null) {
            EstadoAlerta nuevo = parseEstado(req.getEstado());
            // No-op idempotente: mismo estado → 200 sin cambios (spec FR-006).
            a.setEstado(nuevo);
        }
        return new AlertaResponse(repository.save(a));
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId, boolean esAdmin) {
        Alerta a = buscarConPermiso(id, usuarioId, esAdmin);
        repository.delete(a);
    }

    @Transactional(readOnly = true)
    public Alerta buscarConPermiso(Long id, Long usuarioId, boolean esAdmin) {
        Alerta a = repository.findById(id).orElseThrow(NegocioException::alertaNoEncontrada);
        if (!esAdmin && !a.getUsuarioId().equals(usuarioId)) {
            throw NegocioException.noPropietario();
        }
        return a;
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw NegocioException.validacionInvalida("nombre: debe tener entre 1 y 100 caracteres");
        }
    }

    private void validarPrecios(BigDecimal min, BigDecimal max) {
        if (min != null && min.compareTo(BigDecimal.ZERO) <= 0) {
            throw NegocioException.validacionInvalida("precioMin: debe ser mayor a 0");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) <= 0) {
            throw NegocioException.validacionInvalida("precioMax: debe ser mayor a 0");
        }
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw NegocioException.validacionInvalida("precioMax: debe ser mayor o igual a precioMin");
        }
    }

    private void exigirAlMenosUnCriterio(String ciudad, TipoPropiedad tipo, Operacion operacion,
            BigDecimal min, BigDecimal max) {
        boolean vacia = (ciudad == null || ciudad.isBlank()) && tipo == null && operacion == null
                && min == null && max == null;
        if (vacia) {
            throw NegocioException.validacionInvalida("al menos un criterio (ciudad, tipo, operacion, precioMin, precioMax) es obligatorio");
        }
    }

    private String normalizar(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return null;
        }
        if (ciudad.trim().length() > 100) {
            throw NegocioException.validacionInvalida("ciudad: máximo 100 caracteres");
        }
        return ciudad.trim();
    }

    TipoPropiedad parseTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        try {
            return TipoPropiedad.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw NegocioException.validacionInvalida("tipo: valor inválido '" + tipo + "'");
        }
    }

    Operacion parseOperacion(String operacion) {
        if (operacion == null || operacion.isBlank()) {
            return null;
        }
        try {
            return Operacion.valueOf(operacion.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw NegocioException.validacionInvalida("operacion: valor inválido '" + operacion + "'");
        }
    }

    private EstadoAlerta parseEstado(String estado) {
        try {
            return EstadoAlerta.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw NegocioException.validacionInvalida("estado: debe ser ACTIVA o INACTIVA");
        }
    }
}
