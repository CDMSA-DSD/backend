package dsd.api.cdmsa.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table (name = "alternatives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alternative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rfc_id", nullable = false)
    private RFC rfc;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 4000)
    private String description;

    @Column(length = 2000)
    private String pros;

    @Column(length = 2000)
    private String cons;
}