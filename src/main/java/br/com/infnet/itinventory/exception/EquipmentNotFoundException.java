package br.com.infnet.itinventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.NOT_FOUND) // quando lançar essa exception, a API responde 404
public class EquipmentNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EquipmentNotFoundException(Long id) {
        super(String.format("Equipamento com id %d não encontrado.", id));
    }

    public EquipmentNotFoundException() {
        super("Equipamento não encontrado.");
    }
    public EquipmentNotFoundException(String message) {
        super(message);
    }




}
