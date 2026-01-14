package br.com.infnet.itinventory.search.index;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEquipmentIndexer implements EquipmentIndexer {

    @Override
    public void upsert(Long equipmentId) {

    }

    @Override
    public void delete(Long equipmentId) {

    }
}