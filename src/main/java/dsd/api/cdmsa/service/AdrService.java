package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.AdrResponse;
import dsd.api.cdmsa.dto.CreateAdrRequest;
import dsd.api.cdmsa.model.ADR;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.model.Alternative;
import dsd.api.cdmsa.repository.AdrRepository;
import dsd.api.cdmsa.repository.RfcRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AdrService {

    private final AdrRepository adrRepository;
    private final RfcRepository rfcRepository;

    @Transactional
    public AdrResponse createAdr(CreateAdrRequest request) {
        // eventually put some checks on the request (but we used @Valid so maybe not needed) ...

        RFC rfc = rfcRepository.findById(request.rfcId())
                .orElseThrow(() -> new EntityNotFoundException("RFC not found with id " + request.rfcId()));

        ADR adr = new ADR();
        adr.setTitle(request.title().trim());
        adr.setContext(request.context().trim());
        adr.setDecision(request.decision().trim());
        adr.setConsequences(request.consequences().trim());				// from this create markdown file, generate and store the url of GitHub in the db
        adr.setStatus(request.status());
        adr.setRfc(rfc);
        ADR saved = adrRepository.save(adr);

        return new AdrResponse(
                saved.getTitle(),
                saved.getContext(),
                saved.getDecision(),
                saved.getConsequences(),
                saved.getStatus(),
                saved.getRfc().getId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional (readOnly = true)
    public AdrResponse getAdrById(Long id) {

        ADR adr = adrRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RFC not found with id " + id));

        return new AdrResponse(
                adr.getTitle(),
                adr.getContext(),
                adr.getDecision(),
                adr.getConsequences(),
                adr.getStatus(),
                adr.getRfc().getId(),
                adr.getCreatedAt(),
                adr.getUpdatedAt()
        );
    }


    @Transactional(readOnly = true)
    public Page<AdrResponse> listAdrs(Pageable pageable) {
        return adrRepository.findAll(pageable)
                .map(adr -> new AdrResponse(
                        adr.getTitle(),
                        adr.getContext(),
                        adr.getDecision(),
                        adr.getConsequences(),
                        adr.getStatus(),
                        adr.getRfc().getId(),
                        adr.getCreatedAt(),
                        adr.getUpdatedAt()
                ));
    }

    @Transactional
    public ADR createDraftFromRfcAndAlternative(RFC rfc, Alternative alternative) {
            ADR adr = new ADR();
            adr.setRfc(rfc);
            adr.setStatus(ADR.Status.DRAFT);
            adr.setTitle("ADR for RFC #" + rfc.getId() + ": " + rfc.getTitle());
            adr.setContext(rfc.getDescription() != null ? rfc.getDescription() : "");
            adr.setDecision("Selected alternative: " + (alternative != null ? alternative.getTitle() : ""));
            adr.setConsequences("");
            return adrRepository.save(adr);
    }
}

