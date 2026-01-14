package br.com.infnet.itinventory.search.controller;

import br.com.infnet.itinventory.search.doc.EquipmentDoc;
import br.com.infnet.itinventory.search.dto.EquipmentSearchRequest;
import br.com.infnet.itinventory.search.service.EquipmentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

@ConditionalOnProperty(prefix = "search.es", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/api/equipments/search")
@RequiredArgsConstructor
public class EquipmentSearchController {

    private final EquipmentSearchService searchService;

    /**
     * Busca simples para front: /api/equipments/search?q=DELL&page=0&size=10
     */
    @GetMapping("/search")
    public List<EquipmentDoc> search(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) throws IOException {
        return searchService.buscarPorTexto(q, page, size);
    }

    /**
     * Busca avançada para front: /api/equipments/search/advanced?page=0&size=10
     * Body (JSON): { "texto": "dell", "status": "ATIVO", "type": "NOTEBOOK", ... }
     */
    @PostMapping("/search/advanced")
    public List<EquipmentDoc> advanced(
            @RequestBody EquipmentSearchRequest req,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) throws IOException {
        return searchService.buscaAvancada(req, page, size);
    }
}