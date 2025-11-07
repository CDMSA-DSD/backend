package dsd.api.cdmsa.model;

import java.util.ArrayList;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity // Create a table
@Table(name = "organization") // Name a table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private java.util.List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "org")
    private java.util.List<RFC> rfcs = new ArrayList<>();

}