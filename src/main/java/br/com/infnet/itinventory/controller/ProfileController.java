package br.com.infnet.itinventory.controller;

import br.com.infnet.itinventory.dto.ProfileOptionDTO;
import br.com.infnet.itinventory.dto.ProfileResponseDTO;
import br.com.infnet.itinventory.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileRepository profileRepository;

    /**
     * GET /api/profiles
     * Retorna o DTO completo:
     * [{ idProfile, code, name, descricao, nivelAcesso, ativo }]
     */
    @GetMapping
    public ResponseEntity<List<ProfileResponseDTO>> listAll() {
        List<ProfileResponseDTO> result = profileRepository.findAll()
                .stream()
                .map(ProfileResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/profiles/{id}
     * Retorna o DTO completo de 1 perfil.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponseDTO> getById(@PathVariable Long id) {
        return profileRepository.findById(id)
                .map(ProfileResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/profiles/options
     * Retorna somente (id, name) se você ainda quiser usar em algum select simples.
     * Isso evita conflito com GET /api/profiles.
     */
    @GetMapping("/options")
    public ResponseEntity<List<ProfileOptionDTO>> listOptions() {
        List<ProfileOptionDTO> result = profileRepository.findAll()
                .stream()
                .map(ProfileOptionDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(result);
    }
}