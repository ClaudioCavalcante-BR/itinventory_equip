package br.com.infnet.itinventory.dto;

import br.com.infnet.itinventory.model.Profile;

public record ProfileOptionDTO(
        Long id,
        String name
) {
    public static ProfileOptionDTO fromEntity(Profile p) {

        return new ProfileOptionDTO(p.getId(), p.getName());
    }
}