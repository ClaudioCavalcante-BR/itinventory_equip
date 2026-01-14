package br.com.infnet.itinventory.search.listener;

import br.com.infnet.itinventory.search.event.EquipmentIndexEvent;
import br.com.infnet.itinventory.search.event.EquipmentIndexOperation;
import br.com.infnet.itinventory.search.index.EquipmentIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EquipmentIndexListener {

    private final EquipmentIndexer indexer;

    @Async("searchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentIndexEvent(EquipmentIndexEvent event) {
        try {
            if (event.operation() == EquipmentIndexOperation.DELETE) {
                indexer.delete(event.equipmentId());
            } else {
                indexer.upsert(event.equipmentId());
            }
        } catch (Exception e) {
            // Segurança extra: indexer já trata, mas aqui já garante isolamento total
            log.warn("Falha no listener de indexação equipmentId={} op={}. Motivo={}",
                    event.equipmentId(), event.operation(), e.getMessage(), e);
        }
    }
}