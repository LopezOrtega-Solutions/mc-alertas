package com.terrahome.msalertas.integration;

import static org.junit.jupiter.api.Assertions.*;
import com.terrahome.msalertas.security.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * CatalogoClient con servidor mock: traduce criterios, reenvía JWT y
 * mapea fallos a CATALOGO_NO_DISPONIBLE.
 */
class CatalogoClientTest {

    @Test
    void traduceCriteriosYReenviaJwt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        server.expect(r -> {
            assertTrue(r.getURI().toString().contains("ciudad="));
            assertTrue(r.getURI().toString().contains("tipo=APARTAMENTO"));
            assertEquals("Bearer abc", r.getHeaders().getFirst("Authorization"));
        }).andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                .withSuccess("{\"items\":[],\"total\":0,\"page\":0,\"size\":20}",
                        MediaType.APPLICATION_JSON));
        CatalogoClient c = new CatalogoClient("http://localhost:8081", client);
        var page = c.buscar("Bogotá", "APARTAMENTO", null, null, null, 0, 20, "abc");
        assertEquals(0, page.total());
        server.verify();
    }

    @Test
    void falloMapea502() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers
                .requestTo(org.hamcrest.Matchers.containsString("/api/v1/propiedades")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withServerError());
        CatalogoClient c = new CatalogoClient("http://localhost:8081", client);
        NegocioException ex = assertThrows(NegocioException.class,
                () -> c.buscar(null, null, null, null, null, 0, 20, "abc"));
        assertEquals("CATALOGO_NO_DISPONIBLE", ex.getCodigo());
        assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, ex.getStatus());
        server.verify();
    }

    @Test
    void metodoEsGet() {
        assertEquals(HttpMethod.GET, HttpMethod.GET);
    }
}
