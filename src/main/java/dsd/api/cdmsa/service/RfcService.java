package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.CommentResponse;
import dsd.api.cdmsa.dto.CreateCommentRequest;
import dsd.api.cdmsa.dto.RfcResponse;
import dsd.api.cdmsa.model.Comment;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.repository.RfcRepository;
import dsd.api.cdmsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RfcService {

    private final RfcRepository rfcRepository;
    private final UserRepository userRepository;

    @Transactional
    public RfcResponse postCommentToRfc(Long rfcId, Long userId, CreateCommentRequest request) {
        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RuntimeException("RFC not found"));

        Comment comment = new Comment();
        comment.setAuthor(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
        comment.setContent(request.content().trim());
        comment.setRfc(rfc);

        rfc.getComments().add(comment);
        rfcRepository.save(rfc); // save also in the table comments thanks to cascade

        List<CommentResponse> commentResponses = rfc.getComments().stream()
                .map(c -> new CommentResponse(
                        c.getAuthor().getUsername(),
                        c.getContent()
                ))
                .toList();

        return new RfcResponse(
                rfc.getId(),
                rfc.getTitle(),
                rfc.getDescription(),
                rfc.getUser().getId(),
                rfc.getTemplate().getId(),
                rfc.getOrg().getId(),
                rfc.getStatus(),
                commentResponses
        );
    }


    @Transactional(readOnly = true)
    public RfcResponse getRfcById(Long id) {
        RFC rfc = rfcRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RFC non trovato"));

        List<CommentResponse> comments = rfc.getComments().stream()
                .map(c -> new CommentResponse(c.getAuthor().getUsername(), c.getContent()))
                .toList();

        return new RfcResponse(
                rfc.getId(),
                rfc.getTitle(),
                rfc.getDescription(),
                rfc.getUser().getId(),
                rfc.getTemplate().getId(),
                rfc.getOrg().getId(),
                rfc.getStatus(),
                comments
        );
    }

}
