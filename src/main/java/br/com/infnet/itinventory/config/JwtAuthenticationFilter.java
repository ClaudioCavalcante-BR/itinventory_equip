package br.com.infnet.itinventory.config;

import br.com.infnet.itinventory.model.User;
import br.com.infnet.itinventory.repository.UserRepository;
import br.com.infnet.itinventory.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(TokenService tokenService,
                                   UserRepository userRepository,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }



    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // libera rotas públicas (login + actuator + error)
        return path.equals("/auth/login")
                || path.equals("/api/auth/login")
                || path.equals("/api/usuarios/login")
                || path.startsWith("/actuator")
                || path.equals("/error");
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        //String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        String authHeader = request.getHeader("Authorization");

        // Se não tem Bearer, segue a cadeia normalmente (endpoint público ou sem auth)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            // 1) Token vazio ou inválido/expirado
            if (token.isBlank() || !tokenService.isValid(token)) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new BadCredentialsException("Token inválido ou expirado")
                );
                return;
            }

            // 2) Subject -> userId
            Long userId;
            try {
                userId = Long.valueOf(tokenService.getSubject(token));
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new BadCredentialsException("Token com subject inválido", e)
                );
                return;
            }

            // 3) Carregar usuário
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BadCredentialsException("Usuário do token não encontrado"));

            // 4) Validar Profile (evitar NPE + bloquear perfil inativo)
            var p = user.getProfile();
            if (p == null) {
                throw new BadCredentialsException("Usuário sem perfil associado");
            }
            if (!Boolean.TRUE.equals(p.getAtivo())) {
                throw new BadCredentialsException("Perfil inativo");
            }

            // 5) Authorities: ROLE_<CODE> + (opcional) LEVEL_<NIVEL>
            String roleName = "ROLE_" + p.getCode(); // ex.: ROLE_ADMIN
            String level = "LEVEL_" + (p.getNivelAcesso() == null ? 0 : p.getNivelAcesso()); // ex.: LEVEL_3

            var authorities = List.of(
                    new SimpleGrantedAuthority(roleName),
                    new SimpleGrantedAuthority(level)
            );

            // 6) Setar autenticação no contexto do Spring Security
            var authentication = new UsernamePasswordAuthenticationToken(
                    String.valueOf(user.getId()), // principal = userId (String)
                    null,
                    authorities
            );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // segue a cadeia
            filterChain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, ex);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Falha ao processar autenticação JWT", ex)
            );
        }
    }
}





