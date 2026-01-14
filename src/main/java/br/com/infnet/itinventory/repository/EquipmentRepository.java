package br.com.infnet.itinventory.repository;

import br.com.infnet.itinventory.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByAssetNumber(String assetNumber);


}
