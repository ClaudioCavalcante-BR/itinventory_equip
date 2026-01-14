package br.com.infnet.itinventory.exception;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        String path = safePath(request);
        String method = safeMethod(request);

        // Mensagem padrão (qualquer 403)
        String defaultMessage = "Você não tem permissão para executar esta operação.";

        // Regras: quando casar, retorna a mensagem correspondente (primeira regra vencedora)
        String message = DeniedMessageRules.resolve(method, path, defaultMessage);

        ApiError body = new ApiError(
                OffsetDateTime.now(),
                HttpServletResponse.SC_FORBIDDEN,
                "Acesso negado",
                message,
                path
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), body);
    }

    private String safePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null ? "" : uri.replaceAll("[\\r\\n\\t]", "");
    }

    private String safeMethod(HttpServletRequest request) {
        String m = request.getMethod();
        return m == null ? "" : m.replaceAll("[\\r\\n\\t]", "");
    }

    /**
     * Regras de mensagem para 403. Mantém a lógica fora do metodo handle e evita if/else em cadeia.
     */
    private static final class DeniedMessageRules {

        private record Ctx(String method, String path) { }

        private record Rule(Predicate<Ctx> when, String message) { }

        private static final List<Rule> RULES = List.of(
                // Usuários
                new Rule(ctx -> isMethod(ctx, "POST") && isExactPath(ctx, "/api/usuarios"),
                        "Acesso negado: somente ADMIN pode cadastrar usuários."),
                new Rule(ctx -> isMethod(ctx, "GET") && startsWith(ctx, "/api/usuarios"),
                        "Acesso negado: somente ADMIN pode visualizar usuários."),

                // Profiles (para preencher select no front)
                new Rule(ctx -> isMethod(ctx, "GET") && startsWith(ctx, "/api/profiles"),
                        "Acesso negado: somente ADMIN pode visualizar perfis."),

                // Equipments
                new Rule(ctx -> isMethod(ctx, "DELETE") && startsWith(ctx, "/api/equipments"),
                        "Acesso negado: somente ADMIN pode excluir equipamentos."),
                new Rule(ctx -> isMethod(ctx, "PUT") && startsWith(ctx, "/api/equipments"),
                        "Acesso negado: somente ADMIN e GESTOR_TI podem editar equipamentos."),
                new Rule(ctx -> isMethod(ctx, "POST") && startsWith(ctx, "/api/equipments"),
                        "Acesso negado: somente ADMIN, GESTOR_TI e ANALISTA_TI podem cadastrar equipamentos.")
        );

        static String resolve(String method, String path, String defaultMessage) {
            Ctx ctx = new Ctx(method, path);
            return RULES.stream()
                    .filter(r -> r.when.test(ctx))
                    .map(Rule::message)
                    .findFirst()
                    .orElse(defaultMessage);
        }

        private static boolean isMethod(Ctx ctx, String expected) {
            return expected.equalsIgnoreCase(ctx.method());
        }

        private static boolean isExactPath(Ctx ctx, String expectedPath) {
            return expectedPath.equals(ctx.path());
        }

        private static boolean startsWith(Ctx ctx, String prefix) {
            return ctx.path() != null && ctx.path().startsWith(prefix);
        }
    }
}
