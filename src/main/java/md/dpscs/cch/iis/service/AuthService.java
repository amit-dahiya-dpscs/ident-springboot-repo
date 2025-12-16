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

    /**
     * Updated to accept clientIpAddress for auditing.
     */
    public AuthResponse loginUser(LoginRequest loginRequest, String clientIpAddress) {
        try {
            logger.info("Authentication attempt for user: {}", loginRequest.getUsername());

            // Core authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT
            String jwt = jwtUtil.generateToken(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            logger.info("User '{}' authenticated successfully", userDetails.getUsername());

            // CHANGED: Pass IP to audit service
            auditService.logLoginAttempt(loginRequest.getUsername(), clientIpAddress, true);

            return new AuthResponse(jwt, userDetails.getUsername(), userDetails.getAuthorities());

        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsername());

            // CHANGED: Pass IP to audit service
            auditService.logLoginAttempt(loginRequest.getUsername(), clientIpAddress, false);

            throw e;
        }
    }
}