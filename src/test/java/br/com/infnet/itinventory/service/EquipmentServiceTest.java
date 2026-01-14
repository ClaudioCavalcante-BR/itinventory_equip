package br.com.infnet.itinventory.service;

import br.com.infnet.itinventory.exception.EquipmentBusinessException;
import br.com.infnet.itinventory.exception.EquipmentNotFoundException;
import br.com.infnet.itinventory.model.Equipment;
import br.com.infnet.itinventory.model.EquipmentStatus;
import br.com.infnet.itinventory.model.EquipmentType;
import br.com.infnet.itinventory.repository.EquipmentRepository;
import br.com.infnet.itinventory.search.event.EquipmentIndexEvent;
import br.com.infnet.itinventory.search.event.EquipmentIndexOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;


    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private EquipmentService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "esEnabled", true);
    }

    @Test
    void update_shouldNormalizeAssetNumber_andBlockDuplicateFromAnotherRecord() {
        // Dado: existe o equipamento "alvo" (id=10) no banco
        Equipment located = buildValidEquipment("INV-00010");
        located.setId(10L);

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(located));

        // E existe outro registro (id=20) com assetNumber "INV-00013"
        Equipment other = buildValidEquipment("INV-00013");
        other.setId(20L);

        // Quando o update tentar normalizar "INV-13" -> "INV-00013", deve achar duplicidade
        when(equipmentRepository.findByAssetNumber("INV-00013"))
                .thenReturn(Optional.of(other));

        // Quando: tentar atualizar id=10 com assetNumber "INV-13" (normaliza para "INV-00013")
        Equipment incoming = buildValidEquipment("INV-13");

        EquipmentBusinessException ex = assertThrows(
                EquipmentBusinessException.class,
                () -> service.update(10L, incoming)
        );

        // Então: deve bloquear por duplicidade (não deve salvar)
        assertTrue(ex.getMessage().contains("assetNumber já existe: INV-00013"));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void update_shouldNormalizeAssetNumber_andSave_whenNoDuplicateExists() {
        // Dado: existe o equipamento "alvo" (id=10) no banco
        Equipment located = buildValidEquipment("INV-00010");
        located.setId(10L);

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(located));

        // E NÃO existe nenhum outro registro com "INV-00013"
        when(equipmentRepository.findByAssetNumber("INV-00013"))
                .thenReturn(Optional.empty());

        // E o save retorna o próprio objeto (comportamento padrão do mock)
        when(equipmentRepository.save(any(Equipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Quando: update com "INV-13" (normaliza para "INV-00013")
        Equipment incoming = buildValidEquipment("INV-13");

        Equipment updated = service.update(10L, incoming);

        //  ===========================================================================
        // assert (evento publicado)
        ArgumentCaptor<EquipmentIndexEvent> captor = ArgumentCaptor.forClass(EquipmentIndexEvent.class);
        verify(publisher).publishEvent(captor.capture());

        EquipmentIndexEvent event = captor.getValue();
        assertEquals(10L, event.equipmentId());
        assertEquals(EquipmentIndexOperation.UPSERT, event.operation());
        //  ===========================================================================

        // Então: deve salvar e o assetNumber deve estar normalizado
        assertNotNull(updated);
        assertEquals("INV-00013", updated.getAssetNumber());
        verify(equipmentRepository, times(1)).save(any(Equipment.class));
    }

    @Test
    void delete_shouldThrowNotFound_whenIdDoesNotExist() {
        // Dado: id inexistente
        when(equipmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Quando / Então: deve lançar NotFound e não deve chamar delete
        assertThrows(EquipmentNotFoundException.class, () -> service.delete(999L));
        verify(equipmentRepository, never()).delete(any());
    }

    @Test
    void delete_shouldDelete_whenIdExists() {
        // Dado: id existente
        Equipment located = buildValidEquipment("INV-00010");
        located.setId(10L);

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(located));

        // Quando
        service.delete(10L);

        verify(publisher).publishEvent(new EquipmentIndexEvent(10L, EquipmentIndexOperation.DELETE));

        // Então
        verify(equipmentRepository, times(1)).deleteById(10L);;
    }

    /**
     * Monta um Equipment "completo" (compatível com PUT replace),
     * preenchendo todos os campos obrigatórios do validateRequiredFields().
     */
    private Equipment buildValidEquipment(String assetNumber) {
        Equipment e = new Equipment();
        e.setType(EquipmentType.NOTEBOOK);
        e.setBrand("Dell");
        e.setModel("Latitude 5400");
        e.setAssetNumber(assetNumber);
        e.setStatus(EquipmentStatus.EM_USO);
        e.setLocation("TI");
        e.setResponsible("Usuário de teste");
        e.setAcquisitionDate(LocalDate.now());
        e.setAcquisitionValue(new BigDecimal("4500.00"));
        return e;
    }
}
