package com.terrahome.msalertas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.terrahome.msalertas.JwtTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Flujo completo: crear → listar → actualizar/pausar → eliminar → permisos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertasFlujoCompletoIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Value("${app.jwt.secret}")
    String secret;

    @Test
    void flujoCompleto() throws Exception {
        String t = JwtTestHelper.token(secret, "500", "COMPRADOR_ARRENDATARIO");
        String creada = mvc.perform(post("/api/v1/alertas")
                .header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Flujo\",\"ciudad\":\"Bogotá\",\"tipo\":\"CASA\",\"operacion\":\"VENTA\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(creada).get("id").asLong();

        mvc.perform(get("/api/v1/alertas").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mvc.perform(patch("/api/v1/alertas/" + id)
                .header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"INACTIVA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVA"));

        mvc.perform(get("/api/v1/alertas/" + id + "/coincidencias")
                .header("Authorization", "Bearer " + t))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALERTA_INACTIVA"));

        mvc.perform(delete("/api/v1/alertas/" + id).header("Authorization", "Bearer " + t))
                .andExpect(status().isNoContent());
    }
}
