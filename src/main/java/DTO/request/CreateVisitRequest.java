package DTO.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@Builder
public class CreateVisitRequest {
    //Визит может быть без клиентов и без процедур
    private Integer clientId;
    private List<Integer> procedureIds;

    @NotBlank(message = "При создании визита дата обязательна")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$",
             message = "Дата и время должны быть в формате yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String visitDate;
}
