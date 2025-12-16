package md.dpscs.cch.iis.controller;

import jakarta.servlet.http.HttpServletRequest;
import md.dpscs.cch.iis.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/log")
    public ResponseEntity<Void> logUserAction(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String action = payload.get("action");
        String details = payload.get("details");

        // 1. Get Username securely from the session (JWT), not the payload
        //    (Prevent spoofing)
        String username = (userDetails != null) ? userDetails.getUsername() : "UNKNOWN_USER";

        // 2. Get IP Address
        String ipAddress = request.getRemoteAddr();

        // 3. Call the updated Async Service
        auditService.logAction(username, ipAddress, action, details);

        return ResponseEntity.ok().build();
    }
}