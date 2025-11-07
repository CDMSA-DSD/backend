package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rfc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization org;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.UNDER_REVIEW;

    @OneToOne(mappedBy = "rfc", cascade = CascadeType.ALL, orphanRemoval = true)
    private ADR adr;
    
    @OneToMany(mappedBy = "rfc", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Observer> observers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "rfc", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Reviewer> reviewers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "rfc", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Comment> comments = new java.util.HashSet<>();

    @OneToMany(mappedBy = "rfc", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Alternative> alternatives = new java.util.HashSet<>();

    public enum Status {
        UNDER_REVIEW, CLOSED_DECIDED, CLOSED_NON_DECIDED
    }

}
