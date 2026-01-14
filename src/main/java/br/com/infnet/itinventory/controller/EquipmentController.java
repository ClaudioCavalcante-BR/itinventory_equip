package br.com.infnet.itinventory.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import br.com.infnet.itinventory.dto.EquipmentRequestDTO;
import br.com.infnet.itinventory.dto.EquipmentResponseDTO;
import br.com.infnet.itinventory.model.Equipment;
import br.com.infnet.itinventory.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService service;

    @GetMapping
    public Page<EquipmentResponseDTO> findAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        page = Math.max(0, page);
        size = Math.min(200, Math.max(1, size)); // 1..200
        return service.list(page, size).map(this::toResponseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponseDTO> getById(@PathVariable Long id) {
        Equipment equipment = service.seekOrFail(id);
        return ResponseEntity.ok(toResponseDTO(equipment));
    }

    @PostMapping
    public ResponseEntity<EquipmentResponseDTO> create(@Valid @RequestBody EquipmentRequestDTO dto) {
        Equipment saved = service.save(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequestDTO dto
    ) {
        Equipment updated = service.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======= MAPPERS  =======

    private Equipment toEntity(EquipmentRequestDTO dto) {
        Equipment equip = new Equipment();
        equip.setType(dto.getType());
        equip.setBrand(dto.getBrand());
        equip.setModel(dto.getModel());
        equip.setAssetNumber(dto.getAssetNumber());
        equip.setStatus(dto.getStatus());
        equip.setLocation(dto.getLocation());
        equip.setResponsible(dto.getResponsible());
        equip.setAcquisitionDate(dto.getAcquisitionDate());
        equip.setAcquisitionValue(dto.getAcquisitionValue());
        return equip;
    }

    private EquipmentResponseDTO toResponseDTO(Equipment equip) {
        return new EquipmentResponseDTO(
                equip.getId(),
                equip.getType(),
                equip.getBrand(),
                equip.getModel(),
                equip.getAssetNumber(),
                equip.getStatus(),
                equip.getLocation(),
                equip.getResponsible(),
                equip.getAcquisitionDate(),
                equip.getAcquisitionValue()
        );
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv() {

        // exporta até 200 registros
        var page = service.list(0, 200);
        var all = page.getContent();

        String header = "id,type,brand,model,assetNumber,status,location,responsible,acquisitionDate,acquisitionValue";
        String body = all.stream()
                .map(e -> String.join(",",
                        String.valueOf(e.getId()),
                        safe(e.getType()),
                        safe(e.getBrand()),
                        safe(e.getModel()),
                        safe(e.getAssetNumber()),
                        safe(e.getStatus()),
                        safe(e.getLocation()),
                        safe(e.getResponsible()),
                        e.getAcquisitionDate() != null ? e.getAcquisitionDate().toString() : "",
                        e.getAcquisitionValue() != null ? e.getAcquisitionValue().toString() : ""
                ))
                .collect(Collectors.joining("\n"));

        String csv = header + "\n" + body;

        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"equipamentos.csv\"")
                .contentType(MediaType.valueOf("text/csv"))
                .body(bytes);
    }

    private String safe(Object v) {
        if (v == null) return "";
        String s = v.toString();
        s = s.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n")) s = "\"" + s + "\"";
        return s;
    }

}
