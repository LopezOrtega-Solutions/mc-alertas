package com.terrahome.msalertas.model.dto;

import com.terrahome.msalertas.model.entity.Alerta;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlertaResponse {
    private Long id;
    private Long usuarioId;
    private String nombre;
    private String ciudad;
    private String tipo;
    private String operacion;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private String estado;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public AlertaResponse() {}

    public AlertaResponse(Alerta a) {
        this.id = a.getId();
        this.usuarioId = a.getUsuarioId();
        this.nombre = a.getNombre();
        this.ciudad = a.getCiudad();
        this.tipo = a.getTipo() == null ? null : a.getTipo().name();
        this.operacion = a.getOperacion() == null ? null : a.getOperacion().name();
        this.precioMin = a.getPrecioMin();
        this.precioMax = a.getPrecioMax();
        this.estado = a.getEstado().name();
        this.creadoEn = a.getCreadoEn();
        this.actualizadoEn = a.getActualizadoEn();
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getCiudad() { return ciudad; }
    public String getTipo() { return tipo; }
    public String getOperacion() { return operacion; }
    public BigDecimal getPrecioMin() { return precioMin; }
    public BigDecimal getPrecioMax() { return precioMax; }
    public String getEstado() { return estado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
