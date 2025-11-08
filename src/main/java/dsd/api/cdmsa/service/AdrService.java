package dsd.api.cdmsa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dsd.api.cdmsa.dto.AdrRequest;
import dsd.api.cdmsa.model.ADR;
import dsd.api.cdmsa.model.Alternative;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.repository.AdrRepository;

@Service
public class AdrService {

    private final AdrRepository adrRepository;

    public AdrService(AdrRepository adrRepository) {
        this.adrRepository = adrRepository;
    }

    public ADR createAdr(AdrRequest request, RFC rfc) {
        ADR adr = new ADR();
        adr.setTitle(request.getTitle());
        adr.setContext(request.getContext());
        adr.setDecision(request.getDecision());
        adr.setConsequences(request.getConsequences()); // from this create markdown file, generate and store the url of
                                                        // GitHub in the db
        adr.setStatus(request.getStatus());
        adr.setRfc(rfc);
        return adrRepository.save(adr);
    }

    public Optional<ADR> getAdrById(Long id) {
        return adrRepository.findById(id);
    }

    public List<ADR> getAllAdrs() {
        return adrRepository.findAll();
    }

    public ADR createDraftFromRfcAndAlternative(RFC rfc, Alternative alternative) {
        ADR adr = new ADR();
        adr.setRfc(rfc);
        adr.setStatus(ADR.Status.DRAFT);
        adr.setTitle("ADR for RFC #" + rfc.getId() + ": " + rfc.getTitle());
        adr.setContext(rfc.getDescription());
        adr.setDecision("Selected alternative: " + alternative.getTitle());
        return adrRepository.save(adr);
    }

}
