package br.com.infnet.itinventory.search.doc;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentDoc {

    private Long idEquipment;

    private String assetNumber;
    private String type;
    private String status;

    private String brand;
    private String model;

    private String location;
    private String responsible;

    private LocalDate acquisitionDate;
    private Double acquisitionValue;

    // Campo opcional para busca full-text “unificada”
    private String description;
}