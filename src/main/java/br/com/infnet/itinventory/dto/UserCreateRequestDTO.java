package br.com.infnet.itinventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequestDTO(

        @NotBlank @Size(max = 150)
        String name,

        @NotBlank @Size(max = 150)
        String jobTitle,

        @NotBlank @Email @Size(max = 150)
        String email,

        @Size(max = 255)
        String dominio,

        @NotBlank @Size(min = 4, max = 255)
        String password,

        @NotNull
        Long idProfile,

        @NotNull
        Boolean ativo
) {}