package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity // Crea la tabla en la BD
@Table(name = "user_mention") // Da nombre a la tabla
@Getter
@Setter
@NoArgsConstructor // Crea constructor vacío
@AllArgsConstructor // Crea constructor con todos los campos
public class UserMention {
    
    @EmbeddedId
    private UserCommentId id = new UserCommentId();

    @ManyToOne (optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne (optional = false)
    @MapsId("commentId")
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

}
