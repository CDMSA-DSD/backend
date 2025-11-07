package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.CreateCommentRequest;
import dsd.api.cdmsa.dto.RfcResponse;
import dsd.api.cdmsa.service.RfcService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/rfcs")
@RequiredArgsConstructor
public class RfcController {

    private final RfcService rfcService;

    /**
     * Retrieves the current user id from the request.
     * For this sprint we use a simple header-based approach ("X-User-Id"),
     * which should be replaced by a proper authentication mechanism later.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            // For development/demo purposes only.
            // In production, this must be replaced by authenticated user context.
            return 1L;
        }
        return Long.parseLong(header);
    }

    // post a new comment under the rfc identified by id
    @PostMapping("/{id}/comments")
    public ResponseEntity<RfcResponse> postCommentToRfc(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged in ...

        Long userId = getCurrentUserId(httpRequest);
        RfcResponse response = rfcService.postCommentToRfc(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // Get a specific RFC from its id, and shows comments, ...
    @GetMapping("/{id}")
    public ResponseEntity<RfcResponse> getRfcById(@PathVariable Long id, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged in ...

        Long userId = getCurrentUserId(httpRequest);
        RfcResponse response = rfcService.getRfcById(id);
        return ResponseEntity.ok(response);

    }


}
