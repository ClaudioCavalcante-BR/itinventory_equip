package br.com.infnet.itinventory.dto;


import br.com.infnet.itinventory.model.EquipmentStatus;
import br.com.infnet.itinventory.model.EquipmentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentResponseDTO {

    private Long id;
    private EquipmentType type;
    private String brand;
    private String model;
    private String assetNumber;
    private EquipmentStatus status;
    private String location;
    private String responsible;
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionValue;
}