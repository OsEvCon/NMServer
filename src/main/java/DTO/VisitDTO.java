package DTO;

import DTO.response.ProcedureDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitDTO {
    private Integer visitId;
    private LocalDateTime visitDateTime;
    private Integer clientId;

    @Builder.Default
    private List<Integer> proceduresId = new ArrayList<>();

}
