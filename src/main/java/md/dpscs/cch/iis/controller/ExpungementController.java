package md.dpscs.cch.iis.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import md.dpscs.cch.iis.dto.ExpungementRequest;
import md.dpscs.cch.iis.service.ExpungementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/expungement")
@RequiredArgsConstructor
public class ExpungementController {

    private final ExpungementService expungementService;

    @PostMapping("/process")
    public ResponseEntity<?> processExpungement(@Valid @RequestBody ExpungementRequest req,
                                                @AuthenticationPrincipal UserDetails user,
                                                HttpServletRequest request) {

        req.setUsername(user.getUsername());
        req.setUserIp(request.getRemoteAddr());

        // Logic: Check authorities to determine the business unit.
        // Data Integrity takes precedence for Bulk Downgrade capabilities.
        boolean isDataIntegrity = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CCH DATA INTEGRITY".equals(a.getAuthority()));

        if (isDataIntegrity) {
            req.setRequestingUnit("DATA_INTEGRITY");
        } else {
            // Default to Expungement Unit (or whatever standard logic applies)
            req.setRequestingUnit("EXPUNGEMENT_UNIT");
        }

        // Capture the potential warning message from the service
        String warningMessage = expungementService.processExpungement(req);

        Map<String, String> response = new HashMap<>();

        if (warningMessage != null) {
            // Case: Success with Warning (FBI Owned)
            response.put("status", "WARNING");
            response.put("message", "SID deleted successfully.");
            response.put("detail", warningMessage); // "REC IS FBI OWNED - DRS MSG NOT SENT"
        } else {
            // Case: Standard Success
            response.put("status", "SUCCESS");
            response.put("message", "Expungement processed successfully.");
        }

        return ResponseEntity.ok(response);
    }
}