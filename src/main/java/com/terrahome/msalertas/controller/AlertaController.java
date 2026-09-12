package com.terrahome.msalertas.controller;

import com.terrahome.msalertas.integration.CatalogoClient;
import com.terrahome.msalertas.model.dto.ActualizarAlertaRequest;
import com.terrahome.msalertas.model.dto.AlertaResponse;
import com.terrahome.msalertas.model.dto.CrearAlertaRequest;
import com.terrahome.msalertas.model.dto.ListaAlertasResponse;
import com.terrahome.msalertas.service.AlertaService;
import com.terrahome.msalertas.service.CoincidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MS-Alertas (US1–US5). Todas las rutas exigen JWT (spec FR-009).
 * El dueño (usuarioId) sale del sub del token, nunca del body.
 */
@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas", description = "Alertas de interés (MS-Alertas, spec 004)")
public class AlertaController {

    private final AlertaService alertaService;
    private final CoincidenciaService coincidenciaService;

    public AlertaController(AlertaService alertaService, CoincidenciaService coincidenciaService) {
        this.alertaService = alertaService;
        this.coincidenciaService = coincidenciaService;
    }

    @PostMapping
    @Operation(summary = "Crear alerta", description = "US1: nombre + al menos un criterio; inicia ACTIVA")
    public ResponseEntity<AlertaResponse> crear(
            @Valid @RequestBody CrearAlertaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alertaService.crear(request, subjectId(jwt)));
    }

    @GetMapping
    @Operation(summary = "Listar mis alertas", description = "US2: solo propias; admin puede filtrar por usuarioId")
    public ResponseEntity<ListaAlertasResponse> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertaService.listar(subjectId(jwt), usuarioId, esAdmin(jwt), page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de alerta", description = "US2: propia o cualquiera si admin")
    public ResponseEntity<AlertaResponse> obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertaService.obtener(id, subjectId(jwt), esAdmin(jwt)));
    }

    @GetMapping("/{id}/coincidencias")
    @Operation(summary = "Coincidencias", description = "US3: solo ACTIVA; 502 si el catálogo falla")
    public ResponseEntity<CatalogoClient.CatalogoPage> coincidencias(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : jwt.getTokenValue();
        return ResponseEntity.ok(coincidenciaService.coincidencias(id, subjectId(jwt), esAdmin(jwt), page, size, token));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar/pausar alerta", description = "US4: parcial; estado ACTIVA/INACTIVA")
    public ResponseEntity<AlertaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarAlertaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertaService.actualizar(id, request, subjectId(jwt), esAdmin(jwt)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar alerta", description = "US5: definitiva; 204")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        alertaService.eliminar(id, subjectId(jwt), esAdmin(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long subjectId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new com.terrahome.msalertas.security.NegocioException("TOKEN_INVALIDO",
                    "Token con sub no numérico", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean esAdmin(Jwt jwt) {
        return "ADMINISTRADOR".equalsIgnoreCase(jwt.getClaimAsString("rol"));
    }
}
