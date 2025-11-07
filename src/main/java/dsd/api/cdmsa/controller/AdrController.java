package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.AdrRequest;
import dsd.api.cdmsa.model.ADR;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.service.AdrService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/adrs")
public class AdrController {

    private final AdrService adrService;
    private final RfcRepository rfcRepository;

    public AdrController(AdrService adrService, RfcRepository rfcRepository) {
        this.adrService = adrService;
        this.rfcRepository = rfcRepository;
    }

    // Create an ADR associated with an RFC (RFC id is sent from the frontend)
    @PostMapping
    public ResponseEntity<ADR> createAdr(@RequestBody AdrRequest request) {
        // check if user is logged in ...
        RFC rfc = rfcRepository.findById(request.getRfcId())
                .orElseThrow(() -> new RuntimeException("RFC non trovato"));
        ADR savedAdr = adrService.createAdr(request, rfc);
        // build the markdown file from form, push it on GitHub through API, store it in the db (the url)
        // ...
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAdr);
    }


    // Get a specific ADR from its id
    @GetMapping("/{id}")
    public ResponseEntity<ADR> getAdrById(@PathVariable Long id) {
        // check if user is logged in ...
        return adrService.getAdrById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<List<ADR>> getAllAdrs() {
        // check if user is logged in ...
        List<ADR> adrs = adrService.getAllAdrs();
        return ResponseEntity.ok(adrs);
    }


}

