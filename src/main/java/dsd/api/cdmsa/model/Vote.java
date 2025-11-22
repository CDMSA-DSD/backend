package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "alternative_id", nullable = false)
    private Alternative alternative;

    @ManyToOne(optional = false)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @NotNull
    @Column(nullable = false)
    private Boolean outcome;        // true means u like the alternative, false means not

}
