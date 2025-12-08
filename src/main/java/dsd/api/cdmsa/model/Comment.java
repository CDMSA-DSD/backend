package dsd.api.cdmsa.model;

import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    private final int MAX_CONTENT_FOR_NOTI = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rfc_id", nullable = false)
    private RFC rfc;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @NotBlank
    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        java.time.Instant now = java.time.Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.Instant.now();
    }

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Comment> replies = new java.util.HashSet<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserMention> mentions = new java.util.HashSet<>();


    public void addMention(User user){
        UserMention mention = new UserMention(
            new UserCommentId(user.getId(),this.id),
            user,
            this
        );
        this.mentions.add(mention);
        user.getMentions().add(mention);
    }

    public String getSummary(){
        if (content.length() <= MAX_CONTENT_FOR_NOTI) {
            return content;
        }
        
        String cut = content.substring(0, MAX_CONTENT_FOR_NOTI);

        int lastSpace = cut.lastIndexOf("");
        if (lastSpace > 0) {
            cut = cut.substring(0,lastSpace);
        }

        return cut + "...";

    }

}