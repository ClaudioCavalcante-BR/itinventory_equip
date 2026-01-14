package br.com.infnet.itinventory.service;

import br.com.infnet.itinventory.dto.AuthPayload;
import br.com.infnet.itinventory.dto.AuthUserDTO;
import br.com.infnet.itinventory.model.User;
import br.com.infnet.itinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica o usuário a partir de email/senha.
     * - Mantém compatibilidade com senhas legadas em texto puro.
     * - Faz upgrade automático para BCrypt após login bem-sucedido (legado).
     * - Só retorna mensagens específicas de "Usuário inativo" / "Perfil inativo"
     *   quando a senha está correta (padrão mais seguro e ainda demonstrável).
     */
    public AuthPayload authenticate(AuthUserDTO authUserDTO) {

        // 0) Validação mínima do payload
        if (authUserDTO == null
                || authUserDTO.getEmail() == null || authUserDTO.getEmail().isBlank()
                || authUserDTO.getPassword() == null || authUserDTO.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email e senha são obrigatórios");
        }

        String email = authUserDTO.getEmail().trim();
        String raw = authUserDTO.getPassword();

        // 1) Buscar usuário por e-mail (se não existir, não vaza informação)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos"
                ));

        // 2) Validar senha (compatível: BCrypt + legado texto puro)
        String stored = user.getPassword();

        boolean storedIsBCrypt = stored != null && stored.startsWith("$2");

        if (storedIsBCrypt) {
            // já é BCrypt
            if (!passwordEncoder.matches(raw, stored)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
            }
        } else {
            // legado (texto puro)
            if (!raw.equals(stored)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
            }

            // upgrade automático para BCrypt após login válido
            user.setPassword(passwordEncoder.encode(raw));
            userRepository.save(user);
        }

        // 3) bloqueios com mensagens específicas
        if (!Boolean.TRUE.equals(user.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo");
        }

        var p = user.getProfile();
        if (p == null || !Boolean.TRUE.equals(p.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Perfil inativo");
        }

        // 4) Gera token somente após passar em tudo
        String token = tokenService.generateToken(user);

        return new AuthPayload(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getJobTitle(),
                p.getCode(),
                p.getNivelAcesso(),
                Boolean.TRUE.equals(user.getAtivo()),
                Boolean.TRUE.equals(p.getAtivo())
        );
    }
}