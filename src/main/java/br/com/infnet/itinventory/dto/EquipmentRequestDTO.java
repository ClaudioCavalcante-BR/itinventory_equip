package br.com.infnet.itinventory.dto;

import br.com.infnet.itinventory.model.EquipmentStatus;
import br.com.infnet.itinventory.model.EquipmentType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentRequestDTO {

        @NotNull(message = "O tipo do equipamento é obrigatório.")
        private EquipmentType type;

        @NotBlank(message = "A marca é obrigatória.")
        @Size(max = 100, message = "A marca deve ter no máximo 100 caracteres.")
        @Pattern(regexp = "^[^<>]*$", message = "O campo não pode conter os caracteres '<' e '>'.")
        private String brand;

        @NotBlank(message = "O modelo é obrigatório.")
        @Size(max = 100, message = "O modelo deve ter no máximo 100 caracteres.")
        @Pattern(regexp = "^[^<>]*$", message = "O campo não pode conter os caracteres '<' e '>'.")
        private String model;

        @NotBlank(message = "O número de patrimônio (assetNumber) é obrigatório.")
        @Size(max = 9, message = "O número de patrimônio deve ter no máximo 9 caracteres (ex.: INV-00123).")
        @Pattern(
                regexp = "^[A-Z]{3}-\\d{5}$",
                // para aceitar AAA-00 - Basta substiruir o \\d{5} por \\d{1,5}
                // substituir a mensagem  "O número de patrimônio deve seguir o formato XXX-N (1 a 5 dígitos) (ex.: INV-33, INV-00033)."
                message = "O número de patrimônio deve seguir o formato XXX-00000 (ex.: AAA-00123).")
        private String assetNumber;

        @NotNull(message = "O status é obrigatório.")
        private EquipmentStatus status;

        @NotBlank(message = "A localização é obrigatória.")
        @Size(max = 150, message = "A localização deve ter no máximo 150 caracteres.")
        @Pattern(regexp = "^[^<>]*$", message = "O campo não pode conter os caracteres '<' e '>'.")
        private String location;

        @NotBlank(message = "O responsável é obrigatório.")
        @Size(max = 120, message = "O responsável deve ter no máximo 120 caracteres.")
        @Pattern(regexp = "^[^<>]*$", message = "O campo não pode conter os caracteres '<' e '>'.")
        private String responsible;

        @NotNull(message = "A data de aquisição é obrigatória.")
        @PastOrPresent(message = "A data de aquisição não pode estar no futuro.")
        private LocalDate acquisitionDate;

        @NotNull(message = "O valor de aquisição é obrigatório.")
        @DecimalMin(value = "0.01", inclusive = true, message = "O valor de aquisição deve ser maior que zero.")
        private BigDecimal acquisitionValue;

    }
