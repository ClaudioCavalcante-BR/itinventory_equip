package br.com.infnet.itinventory.search.event;

public record EquipmentIndexEvent(Long equipmentId, EquipmentIndexOperation operation)

    {

}