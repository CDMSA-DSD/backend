package dsd.api.cdmsa.model;

import java.util.ArrayList;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity // Create a table
@Table(name = "app_users") // Name a table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization org;

    @NotBlank(message = "Name is mandatory")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Username is mandatory")
    @Column(nullable = false)
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Email is mandatory")
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "user")
    private java.util.List<RFC> rfcs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Observer> observers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Reviewer> reviewers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Comment> comment = new java.util.HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Alternative> alternatives = new java.util.HashSet<>();

}
