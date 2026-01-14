package br.com.infnet.itinventory.model;

import lombok.Getter;

@Getter

public enum EquipmentStatus {

    EM_ESTOQUE("Em estoque"),
    EM_USO("Em uso"),
    RESERVADO("Reservado"),
    EM_MANUTENCAO("Em manutenção"),
    EM_GARANTIA("Em garantia"),
    AGUARDANDO_DESCARTE("Aguardando descarte"),
    DESCARTADO("Descartado"),
    PERDIDO_OU_ROUBADO("Perdido ou roubado");

    private final String description;

    EquipmentStatus(String description) {
        this.description = description;
    }

}
