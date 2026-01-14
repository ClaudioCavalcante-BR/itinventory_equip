package br.com.infnet.itinventory.search.event;

import br.com.infnet.itinventory.model.Equipment;
import br.com.infnet.itinventory.repository.EquipmentRepository;
import br.com.infnet.itinventory.search.service.EquipmentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search.es", name = "enabled", havingValue = "true")
public class EquipmentIndexEventListener {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentSearchService searchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(EquipmentIndexEvent event) {

        if (event == null || event.equipmentId() == null || event.operation() == null) {
            return; // defensivo: não quebra fluxo transacional
        }

        if (event.operation() == EquipmentIndexOperation.UPSERT) {
            Equipment e = equipmentRepository.findById(event.equipmentId()).orElse(null);
            if (e != null) {
                searchService.upsert(e);
            }
            return;
        }

        // DELETE
        searchService.deleteById(event.equipmentId());
    }
}