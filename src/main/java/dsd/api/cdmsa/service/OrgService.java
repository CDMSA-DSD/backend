package dsd.api.cdmsa.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Map;

import org.springframework.data.domain.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dsd.api.cdmsa.controller.UserController;
import dsd.api.cdmsa.dto.OrgAdminRequest;
import dsd.api.cdmsa.dto.OrgAdminResponse;
import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.dto.SignInRequest;
import dsd.api.cdmsa.dto.UpdateOrganizationRequest;
import dsd.api.cdmsa.exception.OrgExistsException;
import dsd.api.cdmsa.exception.OrgNotFoundException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.OrganizationRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrgService {

    private final OrganizationRepository orgRepo;
    private final UserService userService;

    public boolean existOrg(String name) {
        return orgRepo.existsByName(name);
    }

    public boolean existOrgById(Long id) {
        return orgRepo.existsById(id);
    }

    @Transactional
    public OrgAdminResponse createOrg(OrgAdminRequest dto) {

        OrganizationResponse org = dto.org();
        SignInRequest user = dto.admin();

        // Check if a Org already exist
        if (!existOrg(org.companyName())) {

            // Creates the org (set all the parameters)
            Organization newOrg = new Organization();
            newOrg.setName(org.companyName());
            newOrg.setDescription(org.description());
            newOrg.setDomain(org.domain());

            // Stores temporal
            newOrg = orgRepo.save(newOrg);

            // Creates the admin (set all the parameters)
            User admin = new User();
            admin.setFirstname(user.firstname());
            admin.setLastname(user.lastname());
            admin.setEmail(user.email());
            admin.setPassword(user.password()); // Hashed later in createUser

            admin.setOrg(newOrg); // Admin belongs to org

            // Stores both resources
            admin = userService.createUser(admin);

            newOrg.setAdminUser(admin); // Admin manages org

            newOrg = orgRepo.save(newOrg); // Updated org

            // Create the admin's URI
            Map<String, Object> adminUri = Map.of(
                    "admin", Map.of(
                            "href", linkTo(UserController.class).slash(admin.getId()).toUri().toString()));

            return new OrgAdminResponse(newOrg.getId(), adminUri);

        }
        // Instead throw a exception that return 409- CONFLICT
        throw new OrgExistsException(org.companyName());
    }

    @Transactional(readOnly = true)
    public Organization getOrgDetails(Long orgId) {
        return orgRepo.findById(orgId).orElseThrow(() -> new OrgNotFoundException(orgId));
    }

    @Transactional
    public Organization updateOrgDetails(Long orgId, UpdateOrganizationRequest request) {
        Organization org = getOrgDetails(orgId);

        org.setName(request.companyName().trim());
        org.setDescription(request.description().trim());
        org.setDomain(request.domain().trim());
        // org.setGitHubToken(request.gitHubToken().trim());

        return orgRepo.save(org);
    }

    public void deleteOrg(Long id) {
        orgRepo.deleteById(id);
    }

    public Page<Organization> findAllOrgs(Long orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orgRepo.findByOrgId(orgId, pageable);
    }

}
