package com.terrahome.msalertas.integration;

import com.terrahome.msalertas.security.NegocioException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Cliente REST síncrono al catálogo (spec FR-008): traduce criterios a query
 * params, reenvía el JWT, timeout 3 s. Cualquier fallo → 502
 * CATALOGO_NO_DISPONIBLE (nunca lista vacía falsa).
 */
@Component
public class CatalogoClient {

    private final RestClient restClient;
    private final String catalogoUrl;

    @Autowired
    public CatalogoClient(@Value("${catalogo.url:http://localhost:8081}") String catalogoUrl,
            @Value("${catalogo.timeout-ms:3000}") int timeoutMs) {
        this.catalogoUrl = catalogoUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    CatalogoClient(String catalogoUrl, RestClient restClient) {
        this.catalogoUrl = catalogoUrl;
        this.restClient = restClient;
    }

    public CatalogoPage buscar(String ciudad, String tipo, String operacion,
            String precioMin, String precioMax, int page, int size, String jwt) {
        try {
            UriComponentsBuilder uri = UriComponentsBuilder
                    .fromUriString(catalogoUrl + "/api/v1/propiedades")
                    .queryParam("page", page)
                    .queryParam("size", Math.min(Math.max(size, 1), 100));
            if (ciudad != null && !ciudad.isBlank()) {
                uri.queryParam("ciudad", ciudad);
            }
            if (tipo != null && !tipo.isBlank()) {
                uri.queryParam("tipo", tipo);
            }
            if (operacion != null && !operacion.isBlank()) {
                uri.queryParam("operacion", operacion);
            }
            if (precioMin != null) {
                uri.queryParam("precioMin", precioMin);
            }
            if (precioMax != null) {
                uri.queryParam("precioMax", precioMax);
            }
            Map<String, Object> body = restClient.get()
                    .uri(uri.encode().build().toUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return CatalogoPage.from(body);
        } catch (ResourceAccessException e) {
            throw NegocioException.catalogoNoDisponible("timeout o catálogo inalcanzable");
        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            throw NegocioException.catalogoNoDisponible("error del catálogo");
        }
    }

    public record CatalogoPage(List<Map<String, Object>> items, long total, int page, int size) {
        @SuppressWarnings("unchecked")
        static CatalogoPage from(Map<String, Object> body) {
            if (body == null) {
                throw NegocioException.catalogoNoDisponible("respuesta vacía del catálogo");
            }
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.getOrDefault("items", List.of());
            Number total = (Number) body.getOrDefault("total", items.size());
            Number p = (Number) body.getOrDefault("page", 0);
            Number s = (Number) body.getOrDefault("size", items.size());
            return new CatalogoPage(items, total.longValue(), p.intValue(), s.intValue());
        }
    }
}
