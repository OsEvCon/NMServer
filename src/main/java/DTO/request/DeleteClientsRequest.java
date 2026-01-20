package DTO.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class DeleteClientsRequest {

    @NotEmpty(message = "Список Id клиентов не может быть пустым")
    private List<@NotNull Integer> clientIds;
}
