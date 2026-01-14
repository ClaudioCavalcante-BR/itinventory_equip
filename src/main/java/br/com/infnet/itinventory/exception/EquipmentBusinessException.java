package br.com.infnet.itinventory.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 – erro de regra de negócio/validação
public class EquipmentBusinessException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 1L;

    // Mensagem simples de regra de negócio
    public EquipmentBusinessException(String message) {
        super(message);
    }

    // Mensagem + causa raiz
    public EquipmentBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    // Construtor padrão
    public EquipmentBusinessException() {
        super("Erro de negócio ao processar equipamento.");
    }

}
