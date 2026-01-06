package md.dpscs.cch.iis.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import md.dpscs.cch.iis.dto.*;
import md.dpscs.cch.iis.service.IdentUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ident/update")
@RequiredArgsConstructor
public class IdentUpdateController {

    private final IdentUpdateService updateService;

    @PutMapping("/{systemId}/demographics")
    public ResponseEntity<?> updateDemographics(@PathVariable Long systemId,
                                                @Valid @RequestBody UpdateDemographicsRequest req,
                                                @AuthenticationPrincipal UserDetails user,
                                                HttpServletRequest request) {
        updateService.updateDemographics(systemId, req, user.getUsername(), request.getRemoteAddr());
        return ResponseEntity.ok().body("{\"message\": \"Record updated successfully\"}");
    }

    @PutMapping("/{systemId}/true-name")
    public ResponseEntity<?> updateTrueName(@PathVariable Long systemId,
                                            @Valid @RequestBody UpdateNameRequest req,
                                            @AuthenticationPrincipal UserDetails user,
                                            HttpServletRequest request) {
        updateService.updateTrueName(systemId, req, user.getUsername(), request.getRemoteAddr());
        return ResponseEntity.ok().body("{\"message\": \"Record updated successfully\"}");
    }

    @PutMapping("/{systemId}/aliases")
    public ResponseEntity<?> updateAliases(@PathVariable Long systemId,
                                           @RequestBody List<UpdateNameRequest> aliases,
                                           @AuthenticationPrincipal UserDetails user,
                                           HttpServletRequest request) {
        updateService.updateAliases(systemId, aliases, user.getUsername(), request.getRemoteAddr());
        return ResponseEntity.ok().body("{\"message\": \"Record updated successfully\"}");
    }

    @PutMapping("/{systemId}/appended")
    public ResponseEntity<?> updateAppended(@PathVariable Long systemId,
                                            @RequestBody UpdateAppendedIdRequest req,
                                            @AuthenticationPrincipal UserDetails user,
                                            HttpServletRequest request) {
        updateService.updateAppendedIdentifiers(systemId, req, user.getUsername(), request.getRemoteAddr());
        return ResponseEntity.ok().body("{\"message\": \"Record updated successfully\"}");
    }

    @PutMapping("/{systemId}/references")
    public ResponseEntity<?> updateReferences(@PathVariable Long systemId,
                                              @RequestBody List<DocumentDTO> documents,
                                              @AuthenticationPrincipal UserDetails user,
                                              HttpServletRequest request) {
        updateService.updateReferenceData(systemId, documents, user.getUsername(), request.getRemoteAddr());
        return ResponseEntity.ok().body("{\"message\": \"Record updated successfully\"}");
    }
}