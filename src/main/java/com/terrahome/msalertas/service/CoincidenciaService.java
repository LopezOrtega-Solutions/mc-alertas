package com.terrahome.msalertas.service;

import com.terrahome.msalertas.integration.CatalogoClient;
import com.terrahome.msalertas.model.entity.Alerta;
import com.terrahome.msalertas.model.entity.EstadoAlerta;
import com.terrahome.msalertas.security.NegocioException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * US3 — Coincidencias bajo demanda (spec FR-008): verifica alerta ACTIVA y
 * permiso dueño-o-admin, delega al catálogo y filtra defensivamente
 * DISPONIBLE. Preserva el orden del catálogo.
 */
@Service
public class CoincidenciaService {

    private final AlertaService alertaService;
    private final CatalogoClient catalogoClient;

    public CoincidenciaService(AlertaService alertaService, CatalogoClient catalogoClient) {
        this.alertaService = alertaService;
        this.catalogoClient = catalogoClient;
    }

    public CatalogoClient.CatalogoPage coincidencias(Long alertaId, Long usuarioId,
            boolean esAdmin, int page, int size, String jwt) {
        Alerta alerta;
        try {
            alerta = alertaService.buscarConPermiso(alertaId, usuarioId, esAdmin);
        } catch (NegocioException e) {
            throw e;
        }
        if (alerta.getEstado() != EstadoAlerta.ACTIVA) {
            throw NegocioException.alertaInactiva();
        }
        CatalogoClient.CatalogoPage resultado = catalogoClient.buscar(
                alerta.getCiudad(),
                alerta.getTipo() == null ? null : alerta.getTipo().name(),
                alerta.getOperacion() == null ? null : alerta.getOperacion().name(),
                alerta.getPrecioMin() == null ? null : alerta.getPrecioMin().toPlainString(),
                alerta.getPrecioMax() == null ? null : alerta.getPrecioMax().toPlainString(),
                page, size, jwt);

        List<Map<String, Object>> disponibles = resultado.items().stream()
                .filter(p -> "DISPONIBLE".equalsIgnoreCase(String.valueOf(p.getOrDefault("estado", ""))))
                .toList();
        return new CatalogoClient.CatalogoPage(disponibles, disponibles.size(), resultado.page(), resultado.size());
    }
}
