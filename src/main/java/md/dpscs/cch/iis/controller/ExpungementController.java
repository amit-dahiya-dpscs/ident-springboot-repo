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

        expungementService.processExpungement(req);

        return ResponseEntity.ok().body("{\"message\": \"Expungement processed successfully.\"}");
    }
}