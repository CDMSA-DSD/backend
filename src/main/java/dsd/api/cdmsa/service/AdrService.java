package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.AdrResponse;
import dsd.api.cdmsa.dto.CreateAdrRequest;
import dsd.api.cdmsa.dto.UpdateAdrRequest;
import dsd.api.cdmsa.model.ADR;
import dsd.api.cdmsa.model.RFC;
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
                saved.getId(),
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
                adr.getId(),
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
                        adr.getId(),
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
    public AdrResponse updateAdr(Long id, UpdateAdrRequest request) {
        ADR adr = adrRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ADR not found with id " + id));

        adr.setTitle(request.title() != null ? request.title().trim() : null);
        adr.setContext(request.context() != null ? request.context().trim() : null);
        adr.setDecision(request.decision() != null ? request.decision().trim() : null);
        adr.setConsequences(request.consequences() != null ? request.consequences().trim() : null);
        adr.setStatus(request.status());

        ADR saved = adrRepository.save(adr);

        return new AdrResponse(
                saved.getId(),
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
}

