package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rfc_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfcAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfc_id", nullable = false)
    private RFC rfc;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String contentType; // for example application/pdf

    @Column(nullable = false)
    private Long size; // in bytes

}