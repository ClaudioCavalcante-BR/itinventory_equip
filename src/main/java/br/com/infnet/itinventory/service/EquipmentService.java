package br.com.infnet.itinventory.service;

import br.com.infnet.itinventory.search.event.EquipmentIndexEvent;
import br.com.infnet.itinventory.search.event.EquipmentIndexOperation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import br.com.infnet.itinventory.exception.EquipmentBusinessException;
import br.com.infnet.itinventory.exception.EquipmentNotFoundException;
import br.com.infnet.itinventory.model.Equipment;
import br.com.infnet.itinventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ApplicationEventPublisher publisher;
    @Value("${search.es.enabled:false}")
    private boolean esEnabled;


    @Transactional
    public Equipment save(Equipment equipment) {
        validateRequiredFields(equipment);

        equipment.setAssetNumber(normalizeAssetNumber(equipment.getAssetNumber()));

        if (equipmentRepository.findByAssetNumber(equipment.getAssetNumber()).isPresent()) {
            throw new EquipmentBusinessException("assetNumber já existe: " + equipment.getAssetNumber());
        }

        Equipment saved = equipmentRepository.save(equipment);

        if (esEnabled) {
            publisher.publishEvent(new EquipmentIndexEvent(saved.getId(), EquipmentIndexOperation.UPSERT));
        }

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        seekOrFail(id);
        equipmentRepository.deleteById(id);

        if (esEnabled) {
            publisher.publishEvent(new EquipmentIndexEvent(id, EquipmentIndexOperation.DELETE));
        }
    }

    public Optional<Equipment> findById(Long id) {
        if (id == null) {
            throw new EquipmentBusinessException("Id não pode ser nulo.");
        }
        return equipmentRepository.findById(id);
    }

    /**
     * Padrão profissional: buscar ou falhar.
     * Centraliza a regra de 404 para id inexistente.
     */
    public Equipment seekOrFail(Long id) {
        return findById(id).orElseThrow(() -> new EquipmentNotFoundException(id));
    }

    /**
     * PUT = update completo (replace).
     * A entidade localizada é sobrescrita pelos valores do payload (exceto id).
     */
    @Transactional
    public Equipment update(Long id, Equipment newEquipment) {
        if (newEquipment == null) {
            throw new EquipmentBusinessException("Equipamento não pode ser nulo.");
        }

        validateRequiredFields(newEquipment);

        Equipment located = seekOrFail(id);

        String normalized = normalizeAssetNumber(newEquipment.getAssetNumber());

        equipmentRepository.findByAssetNumber(normalized).ifPresent(existing -> {
            if (!existing.getId().equals(located.getId())) {
                throw new EquipmentBusinessException("assetNumber já existe: " + normalized);
            }
        });

        located.setType(newEquipment.getType());
        located.setBrand(newEquipment.getBrand());
        located.setModel(newEquipment.getModel());
        located.setAssetNumber(normalized);
        located.setStatus(newEquipment.getStatus());
        located.setLocation(newEquipment.getLocation());
        located.setResponsible(newEquipment.getResponsible());
        located.setAcquisitionDate(newEquipment.getAcquisitionDate());
        located.setAcquisitionValue(newEquipment.getAcquisitionValue());

        Equipment saved = equipmentRepository.save(located);

        if (esEnabled) {
            publisher.publishEvent(new EquipmentIndexEvent(saved.getId(), EquipmentIndexOperation.UPSERT));
        }

        return saved;
    }

    public Page<Equipment> list(Integer page, Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;

        if (p < 0) throw new EquipmentBusinessException("page não pode ser negativo.");
        if (s < 1) throw new EquipmentBusinessException("size deve ser no mínimo 1.");

        return equipmentRepository.findAll(PageRequest.of(p, s));
    }

    // =========================
    // Validações mínimas
    // =========================
    private void validateRequiredFields(Equipment equipment) {
        if (equipment == null) throw new EquipmentBusinessException("Equipamento não pode ser nulo.");
        if (equipment.getType() == null) throw new EquipmentBusinessException("Tipo é obrigatório.");
        if (equipment.getBrand() == null) throw new EquipmentBusinessException("Marca é obrigatória.");
        if (equipment.getModel() == null) throw new EquipmentBusinessException("Modelo é obrigatório.");
        if (equipment.getStatus() == null) throw new EquipmentBusinessException("Status é obrigatório.");
        if (equipment.getLocation() == null) throw new EquipmentBusinessException("Localização é obrigatória.");
        if (equipment.getResponsible() == null) throw new EquipmentBusinessException("Responsável é obrigatório.");

    }

    private String normalizeAssetNumber(String assetNumber) {
        if (assetNumber == null) return null;

        String s = assetNumber.trim().toUpperCase();

        // Aceita INV-13, INV-0013, INV-00013 etc.
        // e normaliza sempre para INV-00013 (5 dígitos)
        if (s.matches("^[A-Z]{3}-\\d{1,5}$")) {
            String prefix = s.substring(0, 3);
            String digits = s.substring(4); // depois do "AAA-"
            int num = Integer.parseInt(digits); // remove zeros à esquerda
            return prefix + "-" + String.format("%05d", num);
        }

        // Se vier fora do padrão, devolve como está (DTO já valida com regex)
        return s;
    }
}
