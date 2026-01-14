package br.com.infnet.itinventory.dto;


public record AuthPayload(
        String token,
        Long userId,
        String name,
        String email,
        String jobTitle,
        String profileCode,
        Integer nivelAcesso,
        boolean userAtivo,  // ativo do usuário
        boolean profileAtivo   // ativo do profile

) {
}
