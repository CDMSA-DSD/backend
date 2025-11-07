package dsd.api.cdmsa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.List;

import dsd.api.cdmsa.exception.*;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.service.OrgService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/orgs")
@AllArgsConstructor
public class OrgController {

    private final OrgService service;

    //POST Orgs
    @PostMapping()
    ResponseEntity<Void> newOrg(@Valid @RequestBody Organization newOrg) {
        // Check if a Org already exist
        if (!service.existOrg(newOrg.getName())) {
            // Store Org
            Organization org = service.createOrg(newOrg);
            // Return answer
            return ResponseEntity.created(linkTo(OrgController.class).slash(org.getId()).toUri()).build();
        }
        // Instead throw a exception that return 409- CONFLICT
        throw new OrgExistsException(newOrg.getName());
    }

    //GET Org (individual)
    @GetMapping(
        value = "/{id}", 
        produces = { "application/json"}
        )
    public ResponseEntity<Organization> getOrg(@PathVariable Integer id) {
        Organization org = service.searchById(id).orElseThrow(() -> new OrgNotFoundException(id));
        return ResponseEntity.ok(org);
    }

    //GET Orgs (collection)
    @GetMapping(
        value = "", 
        produces = {"application/json"}
        )
    public ResponseEntity<List<Organization>> getOrgs() {
        List<Organization> orgs = service.findOrgs();
        return ResponseEntity.ok(orgs);
    }

    //PUT Org
    @PutMapping("/{id}")
    public ResponseEntity<Void> replaceOrg(@Valid @RequestBody Organization newOrg, @PathVariable Integer id) {
        service.searchById(id).map(Organization -> {
            Organization.setName(newOrg.getName());
            Organization.setDomain(newOrg.getDomain());
            Organization.setDescription(newOrg.getDescription());
            return service.createOrg(Organization);
        }).orElseThrow(() -> new OrgNotFoundException(id));

        return ResponseEntity.noContent().build();
    }

    //DELETE Org
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrg(@PathVariable Integer id) {
        if (service.existOrgById(id)) {
            service.deleteOrg(id);
        } else {
            throw new OrgNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

}