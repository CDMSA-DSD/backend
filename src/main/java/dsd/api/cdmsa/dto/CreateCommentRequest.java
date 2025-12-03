package dsd.api.cdmsa.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
                @NotBlank String content,
                Long parentId,
                List<Long> mentions
// maybe add timestamp?
) {
}
