package mapper;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import DTO.request.UpdateClientRequest;
import model.Client;
import model.Master;
import model.Visit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientMapper {

    /**
     * Преобразование Client -> DTO
     */
    public ClientDTO toDTO(Client client) {
        if (client == null) {return null;}

        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setPhoneNumber(client.getPhoneNumber());
        dto.setEmail(client.getEmail());

        if (client.getVisits() != null && !client.getVisits().isEmpty()) {
             dto.setVisitsId(client.getVisits().stream()
                     .map(Visit::getId)
                     .toList());

        }
        return dto;
    }

    /**
     * Преобразование CreateRequest -> Client
     */
    public Client toEntity (CreateClientRequest request) {
        Client client = new Client();
        client.setName(request.getName());
        client.setPhoneNumber(request.getPhoneNumber());
        client.setEmail(request.getEmail());

        return client;
    }

    /**
     * Обновление Entity из UpdateRequest
     */
    public void updateClient(UpdateClientRequest request, Client client) {
        if (request.getName() != null) {
            client.setName(request.getName());
        }

        if (request.getPhoneNumber() != null) {
            client.setPhoneNumber(request.getPhoneNumber());
        }
    }

    public List<ClientDTO> toDTO(List<Client> clients) {
        return clients.stream()
                .map(this::toDTO)
                .toList();
    }
}
