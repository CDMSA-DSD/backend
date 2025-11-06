package dsd.api.cdmsa.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "adrs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ADR {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 4000)
    private String context;

    @NotBlank
    @Column(nullable = false, length = 4000)
    private String decision;

    @Column(length = 4000)
    private String consequences;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;

    @OneToOne(optional = false)
    @JoinColumn(name = "rfc_id", nullable = false, unique = true)
    private RFC rfc;

    public enum Status {
        DRAFT,
        APPROVED,
        REJECTED,
    }
}