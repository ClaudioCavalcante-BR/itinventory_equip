package br.com.infnet.itinventory.model;

import lombok.Getter;

@Getter
public enum EquipmentType {

    NOTEBOOK("Notebook"),
    DESKTOP("Desktop"),
    SERVIDOR("Servidor"),
    MONITOR("Monitor"),
    IMPRESSORA("Impressora"),
    ROTEADOR("Roteador"),
    SWITCH("Switch"),
    SMARTPHONE("Smartphone");

    private final String description;

    EquipmentType(String description) {
        this.description = description;
    }


}
