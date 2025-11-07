package dsd.api.cdmsa.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity // Create a table
@Table(name = "organization") // Name a table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Name is mandatory")
    @Column(nullable = false)
    private String name;

    @Column
    private String domain;

    @Column
    private String description;

    @OneToOne
    @JoinColumn(name = "admin_user_id", unique = true)
    private User adminUser;

    @OneToMany(mappedBy = "org")
    @JsonIgnore
    private java.util.List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "org")
    @JsonIgnore
    private java.util.List<RFC> rfcs = new ArrayList<>();

}