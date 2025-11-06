package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity // Crea la tabla en la BD
@Table(name = "user_reviewers") // Da nombre a la tabla
@Getter
@Setter
@NoArgsConstructor // Crea constructor vacío
@AllArgsConstructor // Crea constructor con todos los campos
public class Reviewer {
    
    @EmbeddedId
    private UserRFCID id = new UserRFCID();

    @ManyToOne (optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne (optional = false)
    @MapsId("rfcId")
    @JoinColumn(name = "rfc_id", nullable = false)
    private RFC rfc;

}
