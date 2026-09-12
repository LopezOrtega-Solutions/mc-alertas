package com.terrahome.msalertas.model.dto;

import java.util.List;

public class ListaAlertasResponse {
    private List<AlertaResponse> items;
    private long total;
    private int page;
    private int size;

    public ListaAlertasResponse() {}

    public ListaAlertasResponse(List<AlertaResponse> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<AlertaResponse> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
