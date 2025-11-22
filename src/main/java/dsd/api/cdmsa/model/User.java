package dsd.api.cdmsa.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.hateoas.RepresentationModel;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.*;

@Entity // Create a table
@Table(name = "app_users") // Name a table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends RepresentationModel<User> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization org;

    @NotBlank(message = "First name is mandatory")
    @Column(nullable = false)
    private String firstname;

    @NotBlank(message = "Last name is mandatory")
    @Column(nullable = false)
    private String lastname;

    @NotBlank(message = "Password is mandatory")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Email is mandatory")
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Instant joinedAt;

    @OneToMany(mappedBy = "user")
    private List<RFC> rfcs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Observer> observers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reviewer> reviewers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comment = new java.util.HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Alternative> alternatives = new java.util.HashSet<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrganizationInvitation> invitations = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }
}
