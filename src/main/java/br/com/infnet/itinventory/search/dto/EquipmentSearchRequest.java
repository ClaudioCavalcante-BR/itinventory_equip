package br.com.infnet.itinventory.search.dto;


public record EquipmentSearchRequest(
        String texto,
        String status,
        String type,
        String location,
        Double minValue,
        Double maxValue,
        String dateFrom,  // ISO: yyyy-MM-dd (ou yyyy-MM-ddTHH:mm:ssZ se usar datetime no ES)
        String dateTo     // ISO
) {}