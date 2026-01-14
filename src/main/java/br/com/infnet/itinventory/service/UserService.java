package br.com.infnet.itinventory.service;

import br.com.infnet.itinventory.dto.UserCreateRequestDTO;
import br.com.infnet.itinventory.dto.UserResponseDTO;
import br.com.infnet.itinventory.model.Profile;
import br.com.infnet.itinventory.model.User;
import br.com.infnet.itinventory.repository.ProfileRepository;
import br.com.infnet.itinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;


    public Page<User> list(Integer page, Integer size) {
        return userRepository.findAll(PageRequest.of(page, size));
    }

    public User create(UserCreateRequestDTO dto) {

        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        Profile profile = profileRepository.findById(dto.idProfile())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile inválido"));

        if (!Boolean.TRUE.equals(profile.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile inativo");
        }

        User u = new User();
        u.setName(dto.name());
        u.setJobTitle(dto.jobTitle());
        u.setEmail(dto.email());
        u.setDominio(dto.dominio());

        // grava SEMPRE com BCrypt (novo usuário)
        u.setPassword(passwordEncoder.encode(dto.password()));

        u.setProfile(profile);
        u.setAtivo(dto.ativo());

        User saved = userRepository.save(u);
        saved.setPassword(null);
        return saved;
    }

    /**
     * Busca um usuário por ID e zera a senha antes de devolver
     * (para não expor o password na API).
     */
    public User findByIdWithoutPassword(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setPassword(null);
                    return user;
                })
                .orElse(null);
    }
    public UserResponseDTO findMyProfileDTO(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        user.setPassword(null); // opcional (DTO não expõe)
        return UserResponseDTO.fromEntity(user);
    }

    public Page<UserResponseDTO> listarUsuariosDTO(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return page.map(u -> {
            u.setPassword(null); // opcional
            return UserResponseDTO.fromEntity(u);
        });
    }

    public Page<User> listarSemSenha(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        page.forEach(u -> u.setPassword(null));
        return page;
    }

    /**
     * Busca usuário por e-mail.
     * Útil para a autenticação (login).
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Lista todos os usuários, zerando a senha de cada um.
     */
    public List<User> findAllWithoutPassword() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    /**
     * Versão paginada, também limpando a senha.
     */
    public Page<User> findAll(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        page.getContent().forEach(user -> user.setPassword(null));
        return page;
    }

    public List<User> listarTodosSemSenha() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public User update(Long id, br.com.infnet.itinventory.dto.UserUpdateRequestDTO dto) {

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        // valida e-mail (evita duplicidade)
        userRepository.findByEmail(dto.email()).ifPresent(u -> {
            if (!u.getId().equals(existing.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
            }
        });

        Profile profile = profileRepository.findById(dto.idProfile())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile inválido"));

        if (!Boolean.TRUE.equals(profile.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile inativo");
        }

        existing.setName(dto.name());
        existing.setEmail(dto.email());
        existing.setJobTitle(dto.jobTitle());
        existing.setDominio(dto.dominio());
        existing.setProfile(profile);
        existing.setAtivo(dto.ativo());

        // senha opcional: só altera se vier preenchida
        if (dto.password() != null && !dto.password().isBlank()) {
            existing.setPassword(passwordEncoder.encode(dto.password()));
        }

        User saved = userRepository.save(existing);
        saved.setPassword(null);
        return saved;
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }
        userRepository.deleteById(id);
    }

    public void inativar(Long id) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        existing.setAtivo(false);
        userRepository.save(existing);
    }

    public void ativar(Long id) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        existing.setAtivo(true);
        userRepository.save(existing);
    }

    public User setAtivo(Long id, boolean ativo) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setAtivo(ativo);

        User saved = userRepository.save(user);
        saved.setPassword(null);
        return saved;
    }

}
