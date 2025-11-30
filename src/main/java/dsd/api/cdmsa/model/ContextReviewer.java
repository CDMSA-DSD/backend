package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity // Crea la tabla en la BD
@Table(name = "context_reviewers") // Da nombre a la tabla
@Getter
@Setter
@NoArgsConstructor // Crea constructor vacío
@AllArgsConstructor // Crea constructor con todos los campos
public class ContextReviewer {
    
    @EmbeddedId
    private ContextRFCId id = new ContextRFCId();

    @ManyToOne (optional = false)
    @MapsId("contextId")
    @JoinColumn(name = "context_id", nullable = false)
    private Context context;

    @ManyToOne (optional = false)
    @MapsId("rfcId")
    @JoinColumn(name = "rfc_id", nullable = false)
    private RFC rfc;

}
