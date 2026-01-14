package br.com.infnet.itinventory.service;

import br.com.infnet.itinventory.model.Profile;
import br.com.infnet.itinventory.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public List<Profile> listActive() {
        return profileRepository.findByAtivoTrueOrderByNameAsc();
    }
}
