package mapper;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import DTO.request.UpdateClientRequest;
import model.Client;
import model.Master;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientMapper {

    /**
     * Преобразование Client -> DTO
     */
    public ClientDTO toDTOList(Client client) {
        if (client == null) {return null;}

        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setPhoneNumber(client.getPhoneNumber());

        return dto;
    }

    /**
     * Преобразование CreateRequest -> Client
     */
    public Client toClient(CreateClientRequest request, Master currentMaster) {
        Client client = new Client();
        client.setName(request.getName());
        client.setPhoneNumber(request.getPhoneNumber());

        if (currentMaster != null){
            client.setMasters(List.of(currentMaster));
        }

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

    public List<ClientDTO> toDTOList(List<Client> clients) {
        return clients.stream()
                .map(this::toDTOList)
                .toList();
    }
}
