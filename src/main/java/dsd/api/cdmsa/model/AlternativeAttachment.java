package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alternative_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlternativeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternative_id", nullable = false)
    private Alternative alternative;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String contentType;

    private Long size;
}