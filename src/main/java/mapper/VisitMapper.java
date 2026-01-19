package mapper;

import DTO.VisitDTO;
import model.Procedure;
import model.Visit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VisitMapper {

    //Преобразование Visit -> VisitDto
    public VisitDTO toDTO(Visit visit) {
        if (visit == null) {return null;}

        VisitDTO visitDTO = new VisitDTO();
        visitDTO.setVisitId(visit.getId());
        visitDTO.setClientId(visit.getClient().getId());
        visitDTO.setVisitDateTime(visit.getVisitDateTime());

        if (visit.getProcedures() != null &&  !visit.getProcedures().isEmpty()) {
            visitDTO.setProceduresId(visit.getProcedures().stream()
                    .map(Procedure::getId)
                    .toList());
        }

        return visitDTO;
    }

    public List<VisitDTO> toDTO(List<Visit> visits) {
        if (visits == null) {return null;}
        List<VisitDTO> visitDTOs = new ArrayList<>();
        for (Visit visit : visits) {
            visitDTOs.add(toDTO(visit));
        }
        return visitDTOs;
    }
}
