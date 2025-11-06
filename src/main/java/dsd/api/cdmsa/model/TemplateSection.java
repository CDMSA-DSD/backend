package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_section")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TemplateSection {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "template_id", nullable = false)
  private Template template;

  @Column(nullable = false)
  private int position;

  @Column(name = "section_key", nullable = false, length = 50)
  private String key;

  @Column(nullable = false, length = 120)
  private String label;

  @Column(nullable = false, length = 20)
  private String fieldType; 

  @Column(nullable = false)
  private boolean required = false;
}