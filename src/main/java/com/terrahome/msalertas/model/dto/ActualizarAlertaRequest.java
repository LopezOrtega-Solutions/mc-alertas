package com.terrahome.msalertas.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ActualizarAlertaRequest {
    @Size(min = 1, max = 100, message = "nombre: debe tener entre 1 y 100 caracteres")
    private String nombre;

    @Size(max = 100, message = "ciudad: máximo 100 caracteres")
    private String ciudad;

    private String tipo;
    private String operacion;

    @DecimalMin(value = "0.01", message = "precioMin: debe ser mayor a 0")
    private BigDecimal precioMin;

    @DecimalMin(value = "0.01", message = "precioMax: debe ser mayor a 0")
    private BigDecimal precioMax;

    private String estado;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }
    public BigDecimal getPrecioMin() { return precioMin; }
    public void setPrecioMin(BigDecimal precioMin) { this.precioMin = precioMin; }
    public BigDecimal getPrecioMax() { return precioMax; }
    public void setPrecioMax(BigDecimal precioMax) { this.precioMax = precioMax; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
