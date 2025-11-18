package dsd.api.cdmsa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * Represents the membership of a user in a context.
 * It also stores whether the user is a Context Admin for that context.
 */

@Entity
@Table(name = "context_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "context_id", "user_id" })
})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContextMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Context that the user belongs to.
    @ManyToOne(optional = false)
    @JoinColumn(name = "context_id", nullable = false)
    private Context context;

    // User that is member of the context.
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Whether this member is a Context Admin for the given context.
    @Column(name = "is_context_admin", nullable = false)
    private boolean contextAdmin = false;

}
