package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.dto.UpdateOrganizationRequest;
import dsd.api.cdmsa.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/org_details")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;

    /**
     * Retrieves the current user id from the request.
     * For this sprint we use a simple header-based approach ("X-User-Id"),
     * which should be replaced by a proper authentication mechanism later.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            // For development/demo purposes only.
            // In production, this must be replaced by authenticated user context.
            return 1L;
        }
        return Long.parseLong(header);
    }

    // Get organization details
    @GetMapping
    public ResponseEntity<OrganizationResponse> getOrgDetails(HttpServletRequest httpRequest) {
        // TO DO - check if user is logged ...
        Long userId = getCurrentUserId(httpRequest);
        OrganizationResponse org = orgService.getOrgDetails(userId);
        return ResponseEntity.ok(org);
    }

    // Update organization details
    @PutMapping()
    public ResponseEntity<OrganizationResponse> updateOrgDetails(@Valid @RequestBody UpdateOrganizationRequest request, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged ...
        Long userId = getCurrentUserId(httpRequest);
        OrganizationResponse orgUpdated = orgService.updateOrgDetails(userId, request);
        return ResponseEntity.ok(orgUpdated);
    }
}
