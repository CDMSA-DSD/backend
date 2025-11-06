package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.OrganizationRequest;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    private final OrganizationRepository orgRepo;

    public OrganizationService(OrganizationRepository orgRepo) {
        this.orgRepo = orgRepo;
    }

    public Organization getOrgDetails(Long id) {
        return orgRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with id: " + id));
    }

    public Organization updateOrgDetails(Long id, OrganizationRequest request) {
        Organization org = getOrgDetails(id);
        org.setName(request.getCompanyName());
        org.setDescription(request.getDescription());
        org.setDomain(request.getDomain());
        // org.setGitHubToken(request.getGitHubToken());

        return orgRepo.save(org);
    }
}
