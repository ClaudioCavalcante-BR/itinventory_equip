package br.com.infnet.itinventory.service;


import br.com.infnet.itinventory.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    // segredo para assinar o JWT
    @Value("${security.jwt.secret:SEGREDO_JWT_SUPER_SEGURO_123456}")
    private String secret;

    // tempo de expiração em minutos
    @Value("${security.jwt.expiration-minutes:60}")
    private long expirationMinutes;

    /**
     * Gera um JWT com:
     * - subject = id do usuário
     * - claims: name, email, jobTitle (opcional, mas útil)
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        var p = user.getProfile();

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("jobTitle", user.getJobTitle())
                .claim("profileCode", p != null ? p.getCode() : null)
                .claim("nivelAcesso", p != null ? p.getNivelAcesso() : null)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Verifica se o token:
     * - está bem formado
     * - foi assinado com o mesmo secret
     * - não está expirado
     */
    public boolean isValid(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            Date expiration = jws.getBody().getExpiration();
            boolean valido = (expiration == null || expiration.after(new Date()));

            System.out.println("[TokenService] Token valido? " + valido +
                    " | subject=" + jws.getBody().getSubject() +
                    " | exp=" + expiration);

            return valido;

        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("[TokenService] Token inválido: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna o subject do token (id do usuário em String),
     * exatamente como o controller já espera hoje.
     */
    public String getSubject(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);

        return jws.getBody().getSubject();
    }

    // chave HMAC baseada no secret
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
