package md.dpscs.cch.iis.controller;

import md.dpscs.cch.iis.service.AuditService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> logUserAction(@RequestBody Map<String, String> payload) {
        String action = payload.get("action");
        String details = payload.get("details");

        // The user must be authenticated to log an action, so the username
        // will be available in the SecurityContext.
        auditService.logAction(action, details);

        return ResponseEntity.ok().build();
    }
}