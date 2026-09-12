package com.terrahome.msalertas.service;

import static org.junit.jupiter.api.Assertions.*;
import com.terrahome.msalertas.model.dto.ActualizarAlertaRequest;
import com.terrahome.msalertas.model.dto.AlertaResponse;
import com.terrahome.msalertas.model.dto.CrearAlertaRequest;
import com.terrahome.msalertas.model.dto.ListaAlertasResponse;
import com.terrahome.msalertas.security.NegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * US1, US2, US4, US5 a nivel de servicio (H2).
 */
@SpringBootTest
@ActiveProfiles("test")
class AlertaServiceTest {

    @Autowired
    AlertaService service;

    private CrearAlertaRequest nueva(String nombre, String ciudad) {
        CrearAlertaRequest r = new CrearAlertaRequest();
        r.setNombre(nombre);
        r.setCiudad(ciudad);
        r.setTipo("APARTAMENTO");
        r.setOperacion("VENTA");
        r.setPrecioMin(new BigDecimal("200000000"));
        r.setPrecioMax(new BigDecimal("350000000"));
        return r;
    }

    @Test
    void crearAsignaDuenoYActiva() {
        AlertaResponse res = service.crear(nueva("Apto Bogotá", "Bogotá"), 7L);
        assertEquals(7L, res.getUsuarioId());
        assertEquals("ACTIVA", res.getEstado());
    }

    @Test
    void crearSinCriteriosRechaza() {
        CrearAlertaRequest r = new CrearAlertaRequest();
        r.setNombre("Vacía");
        NegocioException ex = assertThrows(NegocioException.class, () -> service.crear(r, 1L));
        assertEquals("VALIDACION_INVALIDA", ex.getCodigo());
    }

    @Test
    void crearRangoInvertidoRechaza() {
        CrearAlertaRequest r = nueva("X", "Bogotá");
        r.setPrecioMin(new BigDecimal("500"));
        r.setPrecioMax(new BigDecimal("100"));
        assertThrows(NegocioException.class, () -> service.crear(r, 1L));
    }

    @Test
    void crearTipoInvalidoRechaza() {
        CrearAlertaRequest r = nueva("X", "Bogotá");
        r.setTipo("CASTILLO");
        assertThrows(NegocioException.class, () -> service.crear(r, 1L));
    }

    @Test
    void listadoAislaPorDueno() {
        service.crear(nueva("A1", "Bogotá"), 101L);
        service.crear(nueva("A2", "Medellín"), 102L);
        ListaAlertasResponse propias = service.listar(101L, null, false, 0, 20);
        assertTrue(propias.getItems().stream().allMatch(a -> a.getUsuarioId() == 101L));
        assertTrue(propias.getItems().stream().noneMatch(a -> a.getUsuarioId() == 102L));
    }

    @Test
    void filtroUsuarioIdSoloAdmin() {
        assertThrows(NegocioException.class, () -> service.listar(1L, 2L, false, 0, 20));
        ListaAlertasResponse res = service.listar(1L, 2L, true, 0, 20);
        assertNotNull(res);
    }

    @Test
    void actualizarPausaReanuda() {
        AlertaResponse creada = service.crear(nueva("Pausable", "Cali"), 55L);
        ActualizarAlertaRequest patch = new ActualizarAlertaRequest();
        patch.setEstado("INACTIVA");
        assertEquals("INACTIVA", service.actualizar(creada.getId(), patch, 55L, false).getEstado());
        patch.setEstado("ACTIVA");
        assertEquals("ACTIVA", service.actualizar(creada.getId(), patch, 55L, false).getEstado());
    }

    @Test
    void actualizarAjena403() {
        AlertaResponse creada = service.crear(nueva("Ajena", "Cali"), 60L);
        ActualizarAlertaRequest patch = new ActualizarAlertaRequest();
        patch.setNombre("Hack");
        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.actualizar(creada.getId(), patch, 61L, false));
        assertEquals("NO_PROPIETARIO", ex.getCodigo());
    }

    @Test
    void eliminarPropiaY404Posterior() {
        AlertaResponse creada = service.crear(nueva("Borrable", "Cali"), 70L);
        service.eliminar(creada.getId(), 70L, false);
        assertThrows(NegocioException.class, () -> service.obtener(creada.getId(), 70L, false));
    }

    @Test
    void eliminarAjena403() {
        AlertaResponse creada = service.crear(nueva("No tuya", "Cali"), 71L);
        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.eliminar(creada.getId(), 72L, false));
        assertEquals("NO_PROPIETARIO", ex.getCodigo());
    }
}
