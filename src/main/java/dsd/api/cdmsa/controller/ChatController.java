package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.ChatBotRequest;
import dsd.api.cdmsa.dto.ChatBotResponse;
import dsd.api.cdmsa.exception.ChatBotException;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatBotResponse> chat(@Valid @RequestBody ChatBotRequest request, @AuthenticationPrincipal UserPrincipal principal) {

        try {
            Long userOrgId = principal.getOrgId();
            String aiResponse = chatService.generateResponse(request.message(), userOrgId);
            return ResponseEntity.ok(new ChatBotResponse(aiResponse));

        } catch (ChatBotException e) {
            // Timeout, VectorStore down, ...
            log.error("Service error: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatBotResponse("Chatbot not available right now"));

        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatBotResponse("Server internal error"));
        }
    }
}