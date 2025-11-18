package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.AdrResponse;
import dsd.api.cdmsa.dto.CreateAdrRequest;
import dsd.api.cdmsa.dto.PublishAdrRequest;
import dsd.api.cdmsa.service.AdrService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.exception.OrgNotFoundException;
import dsd.api.cdmsa.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/adrs")
@RequiredArgsConstructor
public class AdrController {

    private final AdrService adrService;

    private UserPrincipal getAuthenticatedPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        throw new UserNotFoundException("anonymous");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            return getAuthenticatedPrincipal().getUser().getId();
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new UserNotFoundException("anonymous");
        }
    }

    /**
     * Returns the organization id for the current authenticated user.
     */
    private Long getCurrentUserOrgId(HttpServletRequest request) {
        try {
            Long orgId = getAuthenticatedPrincipal().getOrgId();
            if (orgId != null) return orgId;
            throw new OrgNotFoundException(0L);
        } catch (OrgNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new OrgNotFoundException(0L);
        }
    }

    // Create an ADR associated with an RFC (RFC id is sent from the frontend) -- wrote this method with the idea that ADR was filled by the user, see the last method to build it from RFC
    @PostMapping
    public ResponseEntity<AdrResponse> createAdr(@Valid @RequestBody CreateAdrRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        Long orgId = getCurrentUserOrgId(httpRequest);

        AdrResponse response = adrService.createAdr(userId, orgId, request);
        // build the markdown file from form, push it on GitHub through API, store it in the db (the url)
        // ...
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // Get a specific ADR from its id
    @GetMapping("/{id}")
    public ResponseEntity<AdrResponse> getAdrById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long orgId = getCurrentUserOrgId(httpRequest);

        AdrResponse response = adrService.getAdrByIdForOrg(id, orgId);
        return ResponseEntity.ok(response);
    }

    // Update an existing ADR (title, context, decision, consequences, status)
    @PutMapping("/{id}")
    public ResponseEntity<AdrResponse> updateAdr(@PathVariable Long id,
                                                 @Valid @RequestBody dsd.api.cdmsa.dto.UpdateAdrRequest request,
                                                 HttpServletRequest httpRequest) {
        Long orgId = getCurrentUserOrgId(httpRequest);
        AdrResponse response = adrService.updateAdrForOrg(id, orgId, request);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public Page<AdrResponse> listAdrs(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest httpRequest) {
        Long orgId = getCurrentUserOrgId(httpRequest);
        return adrService.listAdrsByOrg(orgId, pageable);
    }

    @PostMapping("/publish")
    public ResponseEntity<AdrResponse> publishAdr(@Valid @RequestBody PublishAdrRequest request, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged in ...
        Long userId = getCurrentUserId(httpRequest);

        // build the markdown file from form, push it on GitHub through API, store it in the db (the url)
        AdrResponse response = adrService.publishAdr(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/publish")
    public ResponseEntity<AdrResponse> publishAdr(@Valid @RequestBody PublishAdrRequest request, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged in ...
        Long userId = getCurrentUserId(httpRequest);

        // build the markdown file from form, push it on GitHub through API, store it in the db (the url)
        AdrResponse response = adrService.publishAdr(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* it is necessary?

    public ADR createDraftFromRfcAndAlternative(RFC rfc, Alternative alternative) {
        ADR adr = new ADR();
        adr.setRfc(rfc);
        adr.setStatus(ADR.Status.DRAFT);
        adr.setTitle("ADR for RFC #" + rfc.getId() + ": " + rfc.getTitle());
        adr.setContext(rfc.getDescription());
        adr.setDecision("Selected alternative: " + alternative.getTitle());
        return adrRepository.save(adr);
    }

     */
}

