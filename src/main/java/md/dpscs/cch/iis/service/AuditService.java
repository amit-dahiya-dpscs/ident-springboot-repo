package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.model.AuditLog;
import md.dpscs.cch.iis.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

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
    public void logAction(String action, String details) {
        String username = getUsernameFromSecurityContext();
        if (username == null) {
            logger.warn("Could not determine username for audit log. Action: {}", action);
            return;
        }

        AuditLog logEntry = new AuditLog(username, action, details);
        auditLogRepository.save(logEntry);
        logger.info("Audit Log: User '{}' performed action '{}'", username, action);
    }

    /**
     * A special method for logging login attempts, as the user is not yet in the security context.
     */
    public void logLoginAttempt(String username, boolean success) {
        String action = success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        AuditLog logEntry = new AuditLog(username, action, null);
        auditLogRepository.save(logEntry);
        logger.info("Audit Log: Login attempt for user '{}'. Result: {}", username, success ? "Success" : "Failure");
    }

    private String getUsernameFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }
}
