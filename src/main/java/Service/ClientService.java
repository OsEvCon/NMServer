package Service;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import DTO.request.DeleteClientsRequest;
import DTO.request.UpdateClientRequest;
import exception.BusinessException;
import exception.ClientDataAccessException;
import exception.ResourceNotFoundException;
import exception.ClientSaveException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mapper.ClientMapper;
import model.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor  // ← Lombok создает конструктор для final полей
@Slf4j   // Lombok создает объект log для логирования
public class ClientService {

    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ClientMapper clientMapper;

    public List<ClientDTO> getClientsForCurrentMaster() {
        log.info("Получение списка клиентов для текущего мастера");

        try {
            Master master = getCurrentMaster();
            List<Client> clients = clientRepository.findByMastersContaining(master);
            log.debug("Найдено {} клиентов для мастера с ID:{}",
                    clients.size(), master.getId());
            return clientMapper.toDTO(clients);
        } catch (Exception e) {
            log.error("Ошибка получения клиентов", e);
            throw new ClientDataAccessException(e);
        }

    }

    public ClientDTO createClient(CreateClientRequest request) {
        log.debug("Создание клиента из запроса: {}", request);

        try {
            Master master = getCurrentMaster();

            boolean phoneExists = clientRepository.existsByMastersContainingAndPhoneNumber(
                    master, request.getPhoneNumber()
            );

            if (phoneExists) {
                throw new BusinessException("У вас уже есть клиент с телефоном " + request.getPhoneNumber());
            }

            Client client = clientMapper.toClient(request, master);
            Client savedClient = clientRepository.save(client);

            ClientDTO result = clientMapper.toDTO(savedClient);

            messagingTemplate.convertAndSend("/topic/clients.update",
                    Map.of(
                            "type", "CREATED",
                            "client", result
                    ));

            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка сохранения клиента", e);
            throw new ClientSaveException(e);
        }
    }

    public void deleteMultipleClients(DeleteClientsRequest  request) {
        Master master = getCurrentMaster();

        List<Integer> clientIds = request.getClientIds();

        Iterable<Client> clientsIterable = clientRepository.findAllById(clientIds);
        List<Client> clientsToDelete = new ArrayList<>();
        clientsIterable.forEach(clientsToDelete::add);

        if (clientsToDelete.isEmpty()) {
            log.warn("Клиенты с указанными ID не найдены {}", clientIds);
            return;
        }

        clientsToDelete = clientsToDelete.stream()
                .filter(client -> client.getMasters().contains(master))
                .toList();

        if (clientsToDelete.isEmpty()) {
            log.warn("Попытка удалить клиентов, не принадлежащих мастеру! IDs: {}", clientIds);
            return; // или бросить исключение
        }

        log.info("Удаление {} клиентов мастера ID: {}",
                clientsToDelete.size(), master.getId());

        detachClientsFromVisits(clientsToDelete);

        for (Client client : clientsToDelete) {
            master.getClients().remove(client);
            client.getMasters().remove(master);

            if (client.getMasters().isEmpty()) {
                clientRepository.delete(client);
                log.debug("Клиент ID: {} удален полностью", client.getId());
            } else {
                clientRepository.save(client);
                log.debug("Клиент ID: {} отвязан от мастера", client.getId());
            }
        }

        List<ClientDTO> deletedDTOs = clientMapper.toDTO(clientsToDelete);
        messagingTemplate.convertAndSend("/topic/clients.update",
                Map.of(
                        "type", "DELETED",
                        "clients", deletedDTOs
                ));
    }

    public ClientDTO updateClient(Integer clientId, UpdateClientRequest request) {
        log.info("Обновление клиента ID: {} c данными {}", clientId, request);

        Master master = getCurrentMaster();
        Client client = clientRepository.findByIdAndMastersContaining(clientId, master)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Клиент с ID " + clientId + " не найден или не принадлежит вам"));

        updateClientFields(master, client, request);

        Client updatedClient = clientRepository.save(client);
        log.info("Клиент ID: {} успешно обновлен", clientId);

        ClientDTO newData = clientMapper.toDTO(updatedClient);

        messagingTemplate.convertAndSend("/topic/clients.update",
                Map.of(
                        "type", "UPDATED",
                        "client", newData
                ));

        return newData;
    }

    private void updateClientFields(Master master, Client client, UpdateClientRequest request) {
        if (request.getName() != null) {
            client.setName(request.getName());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(client.getPhoneNumber())) {

            boolean phoneExists = clientRepository.existsByMastersAndPhoneNumberExcludingId(master, request.getPhoneNumber(), client.getId());

            if (phoneExists) {
                throw new BusinessException(
                        "У вас уже есть клиент с телефоном: " + request.getPhoneNumber());
            }

            client.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getEmail() != null && !request.getEmail().equals(client.getEmail())) {
            client.setEmail(request.getEmail());
        }
    }

    private void detachClientsFromVisits(List<Client> clientsToDelete) {
        for (Client client : clientsToDelete) {
            List<Visit> clientVisits = visitRepository.findVisitsByClient(client)
                            .orElse(Collections.emptyList());

            clientVisits.forEach(visit -> visit.setClient(null));
            visitRepository.saveAll(clientVisits);
        }
    }

    private Master getCurrentMaster() {
        return SecurityUtils.getCurrentMaster();
    }
}
