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

@Entity
@Table(name = "template_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSection {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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