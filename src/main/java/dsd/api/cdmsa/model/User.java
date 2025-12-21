package dsd.api.cdmsa.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class User {
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
    @Column
    private String password;

    @NotBlank(message = "Email is mandatory")
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Instant joinedAt;

    @Column(name = "job_title", nullable = false)
    private String jobTitle = "Developer";  // Developer as default

    @Column
    private String provider;

    @Column
    private String providerUserId;

    @OneToMany(mappedBy = "user")
    private List<RFC> rfcs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Observer> observers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserReviewer> reviewers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comment = new java.util.HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Alternative> alternatives = new java.util.HashSet<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrganizationInvitation> invitations = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserMention> mentions = new java.util.HashSet<>();

    @OneToMany(mappedBy = "targetUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }
}
