package br.com.infnet.itinventory.repository;

import br.com.infnet.itinventory.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByCode(String code);

    // Para preencher o select do cadastro de usuário (idProfile + nome)
    List<Profile> findByAtivoTrueOrderByNameAsc();
}