package com.terrahome.msalertas.security;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {
    private final String codigo;
    private final HttpStatus status;

    public NegocioException(String codigo, String message, HttpStatus status) {
        super(message);
        this.codigo = codigo;
        this.status = status;
    }

    public static NegocioException alertaNoEncontrada() {
        return new NegocioException("ALERTA_NO_ENCONTRADA", "Alerta no encontrada", HttpStatus.NOT_FOUND);
    }

    public static NegocioException noPropietario() {
        return new NegocioException("NO_PROPIETARIO",
                "Solo el dueño de la alerta o un administrador puede realizar esta acción",
                HttpStatus.FORBIDDEN);
    }

    public static NegocioException permisosInsuficientes() {
        return new NegocioException("PERMISOS_INSUFICIENTES", "No autorizado por rol", HttpStatus.FORBIDDEN);
    }

    public static NegocioException alertaInactiva() {
        return new NegocioException("ALERTA_INACTIVA", "La alerta está inactiva", HttpStatus.BAD_REQUEST);
    }

    public static NegocioException validacionInvalida(String detalle) {
        return new NegocioException("VALIDACION_INVALIDA", "Datos inválidos: " + detalle, HttpStatus.BAD_REQUEST);
    }

    public static NegocioException catalogoNoDisponible(String detalle) {
        return new NegocioException("CATALOGO_NO_DISPONIBLE",
                "Catálogo no disponible: " + detalle, HttpStatus.BAD_GATEWAY);
    }

    public String getCodigo() { return codigo; }
    public HttpStatus getStatus() { return status; }
}
