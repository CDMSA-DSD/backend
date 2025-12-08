package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
public class ContextRFCId {
    @Column(name = "context_id", nullable = false)
    private Long contextId;

    @Column(name = "rfc_id", nullable = false)
    private Long rfcId;

}
