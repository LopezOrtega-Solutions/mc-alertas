package com.terrahome.msalertas.model.dto;

public class ApiErrorResponse {
    private String code;
    private String message;

    public ApiErrorResponse() {}

    public ApiErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
