package DTO.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class DeleteClientsRequest {

    @NotEmpty(message = "Список Id клиентов не может быть пустым")
    private List<@NotNull Integer> clientIds;
}
