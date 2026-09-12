package com.terrahome.msalertas.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.terrahome.msalertas.integration.CatalogoClient;
import com.terrahome.msalertas.model.dto.CrearAlertaRequest;
import com.terrahome.msalertas.model.dto.AlertaResponse;
import com.terrahome.msalertas.security.NegocioException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * US3 a nivel de servicio: ACTIVA ok + filtro DISPONIBLE, INACTIVA 400,
 * ajena 403.
 */
@SpringBootTest
@ActiveProfiles("test")
class CoincidenciaServiceTest {

    @Autowired
    AlertaService alertaService;

    @Autowired
    CoincidenciaService coincidenciaService;

    @MockitoBean
    CatalogoClient catalogoClient;

    private CrearAlertaRequest nueva() {
        CrearAlertaRequest r = new CrearAlertaRequest();
        r.setNombre("Apto");
        r.setCiudad("Bogotá");
        r.setTipo("APARTAMENTO");
        r.setOperacion("VENTA");
        r.setPrecioMin(new BigDecimal("100"));
        r.setPrecioMax(new BigDecimal("500"));
        return r;
    }

    @Test
    void filtraDisponible() {
        AlertaResponse a = alertaService.crear(nueva(), 90L);
        when(catalogoClient.buscar(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new CatalogoClient.CatalogoPage(List.of(
                        Map.of("id", 1, "estado", "DISPONIBLE"),
                        Map.of("id", 2, "estado", "VENDIDA")), 2, 0, 20));
        var page = coincidenciaService.coincidencias(a.getId(), 90L, false, 0, 20, "jwt");
        assertEquals(1, page.items().size());
        assertEquals("DISPONIBLE", page.items().get(0).get("estado"));
    }

    @Test
    void inactiva400() {
        AlertaResponse a = alertaService.crear(nueva(), 91L);
        var patch = new com.terrahome.msalertas.model.dto.ActualizarAlertaRequest();
        patch.setEstado("INACTIVA");
        alertaService.actualizar(a.getId(), patch, 91L, false);
        NegocioException ex = assertThrows(NegocioException.class,
                () -> coincidenciaService.coincidencias(a.getId(), 91L, false, 0, 20, "jwt"));
        assertEquals("ALERTA_INACTIVA", ex.getCodigo());
    }

    @Test
    void ajena403() {
        AlertaResponse a = alertaService.crear(nueva(), 92L);
        assertThrows(NegocioException.class,
                () -> coincidenciaService.coincidencias(a.getId(), 93L, false, 0, 20, "jwt"));
    }
}
