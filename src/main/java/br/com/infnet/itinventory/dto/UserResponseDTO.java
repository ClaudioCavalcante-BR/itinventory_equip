package br.com.infnet.itinventory.dto;


import br.com.infnet.itinventory.model.User;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String jobTitle,
        String profileCode,
        Integer nivelAcesso,
        boolean userAtivo,    //ativo do usuário
        boolean profileAtivo
) {
    public static UserResponseDTO fromEntity(User u) {
        var p = u.getProfile();
        return new UserResponseDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getJobTitle(),
                p != null ? p.getCode() : null,
                p != null ? p.getNivelAcesso() : null,
                Boolean.TRUE.equals(u.getAtivo()),              // userAtivo vem do users.ativo
                p != null && Boolean.TRUE.equals(p.getAtivo())  // profileAtivo vem do profile.ativo
        );
    }
}