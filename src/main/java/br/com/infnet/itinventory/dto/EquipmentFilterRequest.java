package br.com.infnet.itinventory.dto;

import br.com.infnet.itinventory.model.EquipmentStatus;
import br.com.infnet.itinventory.model.EquipmentType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentFilterRequest {

    private EquipmentType type;
    private String brand;
    private EquipmentStatus status;
    private String location;
    private String responsible;
}