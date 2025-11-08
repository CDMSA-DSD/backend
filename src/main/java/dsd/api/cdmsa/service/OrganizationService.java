package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.dto.UpdateOrganizationRequest;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.OrganizationRepository;
import dsd.api.cdmsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public OrganizationResponse getOrgDetails(Long id) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();
        return new OrganizationResponse(
                org.getName(),
                org.getDescription(),
                org.getDomain()
        );
    }

    @Transactional
    public OrganizationResponse updateOrgDetails(Long id, UpdateOrganizationRequest request) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();

        org.setName(request.companyName().trim());
        org.setDescription(request.description().trim());
        org.setDomain(request.domain().trim());
        // org.setGitHubToken(request.gitHubToken().trim());

        Organization updatedOrg = orgRepo.save(org);
        return new OrganizationResponse(
                updatedOrg.getName(),
                updatedOrg.getDescription(),
                updatedOrg.getDomain()
        );
    }
}
