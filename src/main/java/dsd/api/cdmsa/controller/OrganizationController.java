package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.OrganizationRequest;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.service.OrganizationService;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orgdetails")
public class OrganizationController {

    private final OrganizationService orgService;

    public OrganizationController(OrganizationService orgService) {
        this.orgService = orgService;
    }

    // Get organization details -- better take the org id from the user, let'see how we implement login (http session or spring security?)
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrgDetails(@PathVariable Long id) {
        // check if user is logged ...
        Organization org = orgService.getOrgDetails(id);
        return ResponseEntity.ok(org);
    }

    // Update organization details -- dto probably substitutable with organization object
    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrgDetails(@PathVariable Long id, @RequestBody OrganizationRequest request) {    // add @Valid?
        // check if user is logged ...
        Organization updated = orgService.updateOrgDetails(id, request);
        return ResponseEntity.ok(updated);
    }
}
