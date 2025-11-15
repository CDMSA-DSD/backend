package dsd.api.cdmsa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.List;

import dsd.api.cdmsa.dto.OrgAdminRequest;
import dsd.api.cdmsa.dto.OrgAdminResponse;
import dsd.api.cdmsa.exception.*;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.service.OrgService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/orgs")
@AllArgsConstructor
public class OrgController {

    private final OrgService service;

    // POST Orgs
    @PostMapping()
    ResponseEntity<OrgAdminResponse> newOrg(@Valid @RequestBody OrgAdminRequest newOrg) {

        // Store Org
        OrgAdminResponse dto = service.createOrg(newOrg);
        // Return answer
        return ResponseEntity.created(linkTo(OrgController.class).slash(dto.getOrg().getId()).toUri()).body(dto);
    }

    // GET Org (individual)
    @GetMapping(value = "/{id}", produces = { "application/json" })
    public ResponseEntity<Organization> getOrg(@PathVariable Long id) {
        Organization org = service.searchById(id).orElseThrow(() -> new OrgNotFoundException(id));
        return ResponseEntity.ok(org);
    }

    // GET Orgs (collection)
    @GetMapping(value = "", produces = { "application/json" })
    public ResponseEntity<List<Organization>> getOrgs() {
        List<Organization> orgs = service.findOrgs();
        return ResponseEntity.ok(orgs);
    }

    // DELETE Org
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrg(@PathVariable Long id) {
        if (service.existOrgById(id)) {
            service.deleteOrg(id);
        } else {
            throw new OrgNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

}