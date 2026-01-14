package br.com.infnet.itinventory.controller;

import br.com.infnet.itinventory.dto.AuthPayload;
import br.com.infnet.itinventory.dto.AuthUserDTO;
import br.com.infnet.itinventory.dto.UserResponseDTO;
import br.com.infnet.itinventory.model.User;
import br.com.infnet.itinventory.service.SecurityService;
import br.com.infnet.itinventory.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final SecurityService securityService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody br.com.infnet.itinventory.dto.UserCreateRequestDTO dto) {
        User saved = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.fromEntity(saved));
    }

    // POST /api/usuarios/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthUserDTO authUserDTO) {
        try {
            AuthPayload payload = securityService.authenticate(authUserDTO);
            return ResponseEntity.ok(payload);

        } catch (org.springframework.web.server.ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(java.util.Map.of(
                            "status", ex.getStatusCode().value(),
                            "error", ex.getReason() != null ? ex.getReason() : "Erro de autenticação"
                    ));
        }
    }

    // GET /api/usuarios?page=0&size=10
    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> listar(
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer size
    ) {
        Page<User> result = userService.listarSemSenha(
                org.springframework.data.domain.PageRequest.of(page, size)
        );

        return ResponseEntity.ok(result.map(UserResponseDTO::fromEntity));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<UserResponseDTO> getMyProfile(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }

        Long userId;
        try {
            userId = Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Principal inválido");
        }

        User user = userService.findByIdWithoutPassword(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }

        if (!Boolean.TRUE.equals(user.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo");
        }

        if (user.getProfile() == null || !Boolean.TRUE.equals(user.getProfile().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Perfil inativo");
        }

        return ResponseEntity.ok(UserResponseDTO.fromEntity(user));
    }

    // GET /api/usuarios/export -> CSV para download
    @GetMapping(value = "/export", produces = "text/csv")
    public void exportUsersCsv(HttpServletResponse response) throws IOException {

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"usuarios.csv\"");

        List<User> users = userService.listarTodosSemSenha();

        var w = response.getWriter();

        // separador ";", compatível com Excel pt-BR
        w.println("id;nome;email;perfil;ativo");

        for (User u : users) {
            String profileName =
                    (u.getProfile() != null && u.getProfile().getName() != null)
                            ? u.getProfile().getName()
                            : "";

            w.printf("%d;%s;%s;%s;%s%n",
                    u.getId(),
                    safeCsv(u.getName()),
                    safeCsv(u.getEmail()),
                    safeCsv(profileName),
                    Boolean.TRUE.equals(u.getAtivo()) ? "SIM" : "NÃO");
        }

        w.flush();
    }

    /**
     * Evita quebrar CSV com separador ';' e nulls.
     */
    private String safeCsv(String s) {
        if (s == null) return "";
        return s.replace(";", " ").replace("\n", " ").replace("\r", " ").trim();
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody br.com.infnet.itinventory.dto.UserUpdateRequestDTO dto
    ) {
        User saved = userService.update(id, dto);
        return ResponseEntity.ok(UserResponseDTO.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // =====================================================
    // PATCH /api/usuarios/{id}/ativar  -> ativo=true (soft delete reversível)
    // PATCH /api/usuarios/{id}/inativar -> ativo=false
    // =====================================================


    @PatchMapping("/{id}/ativar")
    public ResponseEntity<UserResponseDTO> ativar(@PathVariable Long id) {
        User updated = userService.setAtivo(id, true);
        return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<UserResponseDTO> inativar(@PathVariable Long id) {
        User updated = userService.setAtivo(id, false);
        return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
    }


}