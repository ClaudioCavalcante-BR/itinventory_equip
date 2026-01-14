package br.com.infnet.itinventory.search.index;

public interface EquipmentIndexer {

    void upsert(Long equipmentId);
    void delete(Long equipmentId);
}
