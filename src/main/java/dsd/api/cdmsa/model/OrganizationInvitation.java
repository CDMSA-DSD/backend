package dsd.api.cdmsa.model;

import java.time.Instant;

import org.springframework.hateoas.RepresentationModel;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationInvitation extends RepresentationModel<OrganizationInvitation>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization org;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(name = "invited_email") // Future email invitation
    private String email;

    public enum State {
        ACTIVE,
        EXPIRED,
        DISABLED
    }
}
