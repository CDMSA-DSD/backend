package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ADR;
import dsd.api.cdmsa.model.RFC;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AdrRequest {
    // Getters e Setters
    private String title;
    private String context;
    private String decision;
    private String consequences;
    private ADR.Status status;
    private Long rfcId;

}