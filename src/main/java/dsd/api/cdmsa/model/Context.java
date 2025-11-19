package dsd.api.cdmsa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Context represents a logical grouping within an organization
 * (e.g. Team, Project, Product) used for decision ownership and review
 * workflows.
 */
@Entity
@Table(name = "contexts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Context {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String type;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true; // when we soft-delete a context we change active to false
}
