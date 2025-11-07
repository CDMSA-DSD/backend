package dsd.api.cdmsa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.repository.OrgRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrgService {

    private final OrgRepository repository;

    public boolean existOrg(String name) {
        return repository.existsByName(name);
    }

    public Organization createOrg(Organization org) {
        return repository.save(org);
    }

    public Optional<Organization> searchById(int id) {
        return repository.findById(id);
    }

    public List<Organization> findOrgs() {
        return repository.findAll();
    }

    public boolean existOrgById(int id) {
        return repository.existsById(id);
    }

    public void deleteOrg(int id) {
        repository.deleteById(id);
    }

}
