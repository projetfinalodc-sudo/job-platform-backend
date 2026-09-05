package com.odc.plateforme_emploi.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odc.plateforme_emploi.dto.ApiResponse;
import com.odc.plateforme_emploi.exception.TooManyRequestsException;
import com.odc.plateforme_emploi.utils.IpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Protège les routes d'authentification sensibles contre le brute-force et le
 * spam (tentatives de connexion, création de comptes en masse, spam d'e-mails
 * de réinitialisation...). Chaque route a sa propre limite, adaptée à son usage
 * légitime normal (ex: un utilisateur qui se trompe de mot de passe a besoin de
 * plus de tentatives qu'un utilisateur qui crée un compte).
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    private static final Map<String, LimiteConfig> ROUTES_LIMITEES = Map.of(
        "/api/auth/login",             new LimiteConfig(10, 15 * 60 * 1000L),
        "/api/auth/register",          new LimiteConfig(5, 60 * 60 * 1000L),
        "/api/auth/forgot-password",   new LimiteConfig(5, 60 * 60 * 1000L),
        "/api/auth/resend-activation", new LimiteConfig(5, 60 * 60 * 1000L)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        LimiteConfig config = ROUTES_LIMITEES.get(request.getRequestURI());

        if (config != null && "POST".equalsIgnoreCase(request.getMethod())) {
            String cle = IpUtils.extraireIpClient(request) + ":" + request.getRequestURI();
            try {
                rateLimiterService.verifierLimite(cle, config);
            } catch (TooManyRequestsException e) {
                ecrireReponse429(response, e.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void ecrireReponse429(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429); // HttpStatus.TOO_MANY_REQUESTS
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.errorWithCode(message, "RATE_LIMITED");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    // Ne s'applique qu'aux routes listées ci-dessus, laisse passer tout le reste sans overhead.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ROUTES_LIMITEES.containsKey(request.getRequestURI());
    }
}
