package dsd.api.cdmsa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dsd.api.cdmsa.dto.ContextResponse;
import dsd.api.cdmsa.dto.CreateContextRequest;
import dsd.api.cdmsa.dto.UpdateContextRequest;
import dsd.api.cdmsa.service.ContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contexts")
@RequiredArgsConstructor
public class ContextController {

    private final ContextService contextService;

    // ---------------- CHANGE WHEN AUTH IS READY ----------------

    private Long getCurrentUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new RuntimeException("Missing X-User-Id header (temporary auth)");
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid X-User-Id header");
        }
    }

    // ---------------- US-08 Endpoints ----------------

    /*
     * POST /contexts
     * Creates a new context for the current user's organization.
     */

    @PostMapping
    public ResponseEntity<ContextResponse> createContext(
            @RequestBody CreateContextRequest request,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        ContextResponse response = contextService.createContext(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * GET /contexts
     * Lists all contexts for the user's organization.
     */

    @GetMapping
    public ResponseEntity<List<ContextResponse>> listContexts(
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        List<ContextResponse> contexts = contextService.listContexts(userId);
        return ResponseEntity.ok(contexts);
    }

    /*
     * GET /contexts/{id}
     * Retrieves a specific context from the user's organization.
     */

    @GetMapping("/{id}")
    public ResponseEntity<ContextResponse> getContext(
            @PathVariable Long id,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        ContextResponse context = contextService.getContext(userId, id);
        return ResponseEntity.ok(context);
    }

    /*
     * PUT /contexts/{id}
     * Updates a context (admin only).
     */

    @PutMapping("/{id}")
    public ResponseEntity<ContextResponse> updateContext(
            @PathVariable Long id,
            @RequestBody UpdateContextRequest request,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        ContextResponse updated = contextService.updateContext(userId, id, request);
        return ResponseEntity.ok(updated);
    }

    /*
     * DELETE /contexts/{id}
     * Soft deletes a context (admin only).
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContext(
            @PathVariable Long id,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        contextService.deleteContext(userId, id);
        return ResponseEntity.noContent().build();
    }
}
