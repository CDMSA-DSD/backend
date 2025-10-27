package dsd.api.cdmsa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Todo {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;
  private boolean done;
}