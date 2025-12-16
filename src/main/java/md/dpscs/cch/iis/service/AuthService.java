package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.AuthResponse;
import md.dpscs.cch.iis.dto.LoginRequest;
import md.dpscs.cch.iis.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    public AuthResponse loginUser(LoginRequest loginRequest) {
        try {
            logger.info("Authentication attempt for user: {}", loginRequest.getUsername());

            // This is the core authentication step.
            // For LDAP, this will perform the bind and group membership checks.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            // If successful, set the full authentication object (including roles) in the SecurityContext.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate the JWT token.
            String jwt = jwtUtil.generateToken(authentication);

            // Extract the user details from the completed authentication object.
            // This UserDetails object now contains the authorities (roles) populated by our LdapAuthoritiesPopulator.
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            logger.info("User '{}' authenticated successfully with roles: {}", userDetails.getUsername(), userDetails.getAuthorities());

            auditService.logLoginAttempt(loginRequest.getUsername(), true); // Log success

            // Create the response DTO, passing the token, username, and the collection of authorities.
            return new AuthResponse(jwt, userDetails.getUsername(), userDetails.getAuthorities());

        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            auditService.logLoginAttempt(loginRequest.getUsername(), false); // Log failure
            throw e; // Re-throw the exception so the GlobalExceptionHandler can handle it
        }
    }
}