package md.dpscs.cch.iis.service;

import jakarta.servlet.http.HttpServletRequest;
import md.dpscs.cch.iis.model.AuditLog;
import md.dpscs.cch.iis.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Creates and saves an audit log entry.
     * It automatically retrieves the currently authenticated user's name.
     *
     * @param action  A description of the action performed (e.g., "SEARCH_CLICK").
     * @param details Additional JSON details about the action (e.g., the search criteria).
     */
    @Async
    public void logAction(String username, String ipAddress, String action, String details) {
        if (username == null) username = "UNKNOWN";
        if (ipAddress == null) ipAddress = "0.0.0.0";
        AuditLog logEntry = new AuditLog(username, action, details, ipAddress);
        auditLogRepository.save(logEntry);
        logger.info("Audit: User '{}' | IP '{}' | Action '{}'", username, ipAddress, action);
    }

    /**
     * A special method for logging login attempts, as the user is not yet in the security context.
     */
    @Async
    public void logLoginAttempt(String username, String ipAddress, boolean success) {
        String action = success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        AuditLog logEntry = new AuditLog(username, action, null, ipAddress);
        auditLogRepository.save(logEntry);
        logger.info("Audit: Login attempt for '{}' from '{}': {}", username, ipAddress, success);
    }

    private String getUsernameFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwarded = request.getHeader("X-Forwarded-For");
                return xForwarded != null ? xForwarded.split(",")[0] : request.getRemoteAddr();
            }
        } catch (Exception e) {
            return "0.0.0.0";
        }
        return "0.0.0.0";
    }
}
