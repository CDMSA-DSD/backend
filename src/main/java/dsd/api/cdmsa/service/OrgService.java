package dsd.api.cdmsa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dsd.api.cdmsa.exception.OrgExistsException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.payload.OrgAdminRequest;
import dsd.api.cdmsa.payload.OrgAdminResponse;
import dsd.api.cdmsa.repository.OrgRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrgService {

    private final OrgRepository orgRepo;
    private final UserService userService;

    public boolean existOrg(String name) {
        return orgRepo.existsByName(name);
    }

    @Transactional
    public OrgAdminResponse createOrg(OrgAdminRequest dto) {

        // Check if a Org already exist
        if (!existOrg(dto.getOrgName())) {

            // Creates the org (set all the parameters)
            Organization org = new Organization();
            org.setName(dto.getOrgName());
            org.setDescription(dto.getOrgDescription());
            org.setDomain(dto.getOrgDomain());

            // Stores temporal
            org = orgRepo.save(org);
    
            // Creates the admin (set all the parameters)
            User admin = new User();
            admin.setName(dto.getAdminName());
            admin.setUsername(dto.getAdminUsername());
            admin.setEmail(dto.getAdminEmail());
            admin.setPassword(dto.getAdminPassword());
    
            admin.setOrg(org); // Admin belongs to org
    
            // Stores both resources
            admin = userService.createUser(admin);

            org.setAdminUser(admin); // Admin manages org
    
            org = orgRepo.save(org); // Updated org
    
            return new OrgAdminResponse(org, admin);

        }
        // Instead throw a exception that return 409- CONFLICT
        throw new OrgExistsException(dto.getOrgName());
    }

    public Optional<Organization> searchById(int id) {
        return orgRepo.findById(id);
    }

    public List<Organization> findOrgs() {
        return orgRepo.findAll();
    }

    public boolean existOrgById(int id) {
        return orgRepo.existsById(id);
    }

    public void deleteOrg(int id) {
        orgRepo.deleteById(id);
    }

}
