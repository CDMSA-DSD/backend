package dsd.api.cdmsa.model;

import java.util.ArrayList;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity // Create a table
@Table(name = "organization") // Name a table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization extends RepresentationModel<Organization>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String domain;

    @Column
    private String description;

    @Column
    private String gitHubToken;

    @Column
    private String selectedRepoName;

    @Column
    private String selectedBranchName;

    @Column
    private String repoOwner;

    @OneToOne
    @JsonIgnoreProperties({"org"})
    @JoinColumn(name = "admin_user_id", unique = true)
    private User adminUser;

    @OneToMany(mappedBy = "org")
    @JsonIgnore
    private java.util.List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "org")
    @JsonIgnore
    private java.util.List<RFC> rfcs = new ArrayList<>();

}