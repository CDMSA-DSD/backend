package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
public class UserRFCId {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rfc_id", nullable = false)
    private Long rfcId;

}
