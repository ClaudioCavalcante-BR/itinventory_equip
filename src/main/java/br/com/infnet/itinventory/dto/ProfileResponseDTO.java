package br.com.infnet.itinventory.dto;
import br.com.infnet.itinventory.model.Profile;

public record ProfileResponseDTO(
        Long idProfile,
        String code,
        String name,
        String descricao,
        Integer nivelAcesso,
        Boolean ativo
) {
    public static ProfileResponseDTO fromEntity(Profile p) {
        return new ProfileResponseDTO(
                p.getId(),          // id_profile
                p.getCode(),        // ADMIN, GESTOR_TI, ANALISTA_TI, USUARIO
                p.getName(),        // nome amigável
                p.getDescricao(),
                p.getNivelAcesso(),
                p.getAtivo()
        );
    }
}
