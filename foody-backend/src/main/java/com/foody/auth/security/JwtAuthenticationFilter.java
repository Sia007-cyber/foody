package com.foody.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.foody.auth.repository.ImpersonationSessionRepository;

/**
 * Validates the Bearer access token on each request and populates the
 * Spring Security context with a FoodyUserPrincipal.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final FoodyUserDetailsService userDetailsService;
    private final ImpersonationSessionRepository impersonationSessions;

    public JwtAuthenticationFilter(JwtService jwtService, FoodyUserDetailsService userDetailsService,
            ImpersonationSessionRepository impersonationSessions) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.impersonationSessions = impersonationSessions;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                if (!"access".equals(claims.get("typ", String.class))) {
                    throw new com.foody.common.exception.InvalidCredentialsException("Not an access token");
                }
                Long userId = jwtService.getUserId(claims);
                if (SecurityContextHolder.getContext().getAuthentication() == null && userId != null) {
                    if (jwtService.isImpersonation(claims)) {
                        var audit = impersonationSessions.findById(jwtService.getImpersonationSessionId(claims))
                                .orElseThrow(() -> new com.foody.common.exception.InvalidCredentialsException("Unknown impersonation session"));
                        if (audit.getEndedAt() != null || !audit.getTarget().getId().equals(userId)
                                || !audit.getAdmin().getId().equals(jwtService.getInitiatingAdminId(claims))) {
                            throw new com.foody.common.exception.InvalidCredentialsException("Impersonation session has ended");
                        }
                    }
                    var loaded = (FoodyUserPrincipal) userDetailsService.loadByUserId(userId);
                    var principal = jwtService.isImpersonation(claims)
                            ? new FoodyUserPrincipal(loaded.getUser(), jwtService.getInitiatingAdminId(claims),
                                    jwtService.getImpersonationSessionId(claims))
                            : loaded;
                    // Eligibility comes from the current database account, not the JWT.
                    if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
                        throw new com.foody.common.exception.InvalidCredentialsException("Account is suspended or disabled");
                    }
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (com.foody.common.exception.InvalidCredentialsException ex) {
                // Leave the context unauthenticated; the request proceeds and is rejected by
                // authorization rules if it hits a protected endpoint.
            }
        }
        filterChain.doFilter(request, response);
    }
}
