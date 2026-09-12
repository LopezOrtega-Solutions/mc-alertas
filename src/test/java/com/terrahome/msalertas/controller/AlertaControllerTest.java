package com.terrahome.msalertas.controller;

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
 * Contrato REST US1–US5: 201/200/204, 400/401/403/404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertaControllerTest {

    @Autowired
    MockMvc mvc;

    @Value("${app.jwt.secret}")
    String secret;

    String token(Long sub, String rol) {
        return JwtTestHelper.token(secret, String.valueOf(sub), rol);
    }

    @Test
    void post201ConToken() throws Exception {
        mvc.perform(post("/api/v1/alertas")
                .header("Authorization", "Bearer " + token(1L, "COMPRADOR_ARRENDATARIO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Apto\",\"ciudad\":\"Bogotá\",\"tipo\":\"APARTAMENTO\",\"operacion\":\"VENTA\",\"precioMin\":100,\"precioMax\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void post400SinCriterios() throws Exception {
        mvc.perform(post("/api/v1/alertas")
                .header("Authorization", "Bearer " + token(1L, "COMPRADOR_ARRENDATARIO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Vacía\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_INVALIDA"));
    }

    @Test
    void sinToken401() throws Exception {
        mvc.perform(get("/api/v1/alertas")).andExpect(status().isUnauthorized());
    }

    @Test
    void crudPropio403Ajeno404() throws Exception {
        String t1 = token(201L, "COMPRADOR_ARRENDATARIO");
        String t2 = token(202L, "COMPRADOR_ARRENDATARIO");
        String creada = mvc.perform(post("/api/v1/alertas")
                .header("Authorization", "Bearer " + t1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Mía\",\"ciudad\":\"Cali\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(creada).get("id").asLong();

        mvc.perform(get("/api/v1/alertas/" + id).header("Authorization", "Bearer " + t2))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/alertas/999999").header("Authorization", "Bearer " + t1))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/alertas/" + id).header("Authorization", "Bearer " + t2))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/alertas/" + id).header("Authorization", "Bearer " + t1))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/alertas/" + id).header("Authorization", "Bearer " + t1))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchInvalido400() throws Exception {
        String t = token(301L, "COMPRADOR_ARRENDATARIO");
        String creada = mvc.perform(post("/api/v1/alertas")
                .header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"P\",\"ciudad\":\"Cali\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(creada).get("id").asLong();
        mvc.perform(patch("/api/v1/alertas/" + id)
                .header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"CASTILLO\"}"))
                .andExpect(status().isBadRequest());
    }
}
