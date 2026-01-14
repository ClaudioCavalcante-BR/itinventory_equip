package br.com.infnet.itinventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        String name,

        // está usando "jobTitle" como nome amigável do perfil (ex.: "Administrador do Sistema")
        @NotBlank(message = "Perfil (jobTitle) é obrigatório")
        String jobTitle,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        // opcional
        String dominio,

        // opcional: se vier vazio/null, mantém a senha atual
        String password,

        @NotNull(message = "idProfile é obrigatório")
        Long idProfile,

        @NotNull(message = "ativo é obrigatório")
        Boolean ativo
) {}