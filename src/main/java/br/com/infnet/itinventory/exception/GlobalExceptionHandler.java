package br.com.infnet.itinventory.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.Objects;

@Order(Ordered.HIGHEST_PRECEDENCE) // força esse handler a ser avaliado primeiro
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        // Ex.: "listar.page: deve ser maior que ou igual à 0"
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("Parâmetros inválidos");

        ApiError body = new ApiError(
                OffsetDateTime.now(),
                400,
                "Requisição inválida",
                msg,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    @ExceptionHandler(EquipmentNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EquipmentNotFoundException ex, HttpServletRequest req) {
        log.info("Recurso não encontrado. path={}, message={}", safePath(req), safeMessage(ex.getMessage()));
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(EquipmentBusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(EquipmentBusinessException ex, HttpServletRequest req) {
        log.info("Regra de negócio/validação. path={}, message={}", safePath(req), safeMessage(ex.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Regra de negócio/validação", ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenOperationException ex, HttpServletRequest req) {
        log.warn("Operação não permitida. path={}, message={}", safePath(req), safeMessage(ex.getMessage()));
        return build(HttpStatus.FORBIDDEN, "Operação não permitida", ex.getMessage(), req);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.info("Requisição inválida. path={}, message={}", safePath(req), safeMessage(ex.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), req);
    }

    /**
     * Quando o JSON do request está malformado ou com tipos inválidos (ex.: string onde era número).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.info("JSON inválido no request. path={}, cause={}", safePath(req), safeMessage(ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "JSON inválido", "O corpo da requisição está inválido ou malformado.", req);
    }

    /**
     * Quando usar @Valid em DTOs no Controller, esse handler devolve 400 com mensagem amigável.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = "Falha de validação nos campos informados.";

        if (ex.getBindingResult() != null && ex.getBindingResult().hasFieldErrors()) {
            var fe = ex.getBindingResult().getFieldErrors().get(0);

            // Evita &#x27; (escape de aspas simples) e mantém mensagem limpa
            message = String.format("Campo %s: %s", fe.getField(), fe.getDefaultMessage());
            // Alternativa ainda mais simples:
            // message = String.format("Campo %s: %s", fe.getField(), fe.getDefaultMessage());
        }

        log.info("Validação @Valid falhou. path={}, message={}", safePath(req), safeMessage(message));
        return build(HttpStatus.BAD_REQUEST, "Validação de dados", message, req);
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
        String reason = (ex.getReason() != null && !ex.getReason().isBlank())
                ? ex.getReason()
                : "Erro de autenticação";

        return ResponseEntity.status(ex.getStatusCode()).body(java.util.Map.of(
                "status", ex.getStatusCode().value(),
                "error", reason
        ));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        // Log completo (stacktrace) para diagnóstico
        log.error("Erro inesperado. path={}", safePath(req), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.",
                req
        );
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message, HttpServletRequest req) {

        Objects.requireNonNull(status, "status não pode ser nulo.");
        Objects.requireNonNull(error, "error não pode ser nulo.");
        Objects.requireNonNull(req, "request não pode ser nulo.");

        ApiError body = new ApiError(
                OffsetDateTime.now(),
                status.value(),
                error,
                sanitizeForClient(message),
                safePath(req)
        );

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Sanitização simples para reduzir risco de refletir HTML em clientes que renderizem mensagens de forma insegura.
     */
    private String sanitizeForClient(String text) {
        if (text == null) return null;
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String safePath(HttpServletRequest req) {
        return (req == null) ? "" : req.getRequestURI();
    }

    private String safeMessage(String msg) {
        return (msg == null) ? "" : msg;
    }

}
