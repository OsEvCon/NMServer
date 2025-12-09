package Service;

import DTO.ClientDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mapper.ClientMapper;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor  // ← Lombok создает конструктор для final полей
@Slf4j   // Lombok создает объект log для логирования
public class ClientService {
    private final MasterRepository masterRepository;
    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ClientMapper clientMapper;

    public List<ClientDTO> getClientsForCurrentMaster() {
        log.info("Получение списка клиентов для текущего мастера");

        try {
            Master master = getCurrentMaster();
            List<Client> clients = clientRepository.findByMastersContaining(master);
            log.debug("Найдено {} клиентов для мастера {}",
                    clients.size(), master.getId());
            return clientMapper.toDTO(clients);
        } catch (Exception e) {
            log.error("Ошибка получения клиентов", e);
            throw new RuntimeException(e);
        }

    }

    public void deleteMultipleClients(List<Integer> clientIds) {
        Master master = getCurrentMaster();

        List<Client> clientsToDelete = (List<Client>) clientRepository.findAllById(clientIds);
        clientsToDelete = clientsToDelete.stream()
                .filter(client -> client.getMasters().contains(master))
                .toList();

        detachClientsFromVisits(clientsToDelete);

        master.getClients().removeAll(clientsToDelete);
        masterRepository.save(master);

        clientRepository.deleteAll(clientsToDelete);

        messagingTemplate.convertAndSend("/topic/clients.update",
                Map.of(
                        "type", "DELETED",
                        "clients", clientsToDelete
                ));
    }

    private void detachClientsFromVisits(List<Client> clientsToDelete) {
        for (Client client : clientsToDelete) {
            List<Visit> clientVisits = visitRepository.findVisitsByClient(client).get();
            clientVisits.forEach(visit -> visit.setClient(null));
            visitRepository.saveAll(clientVisits);
        }
    }

    private Master getCurrentMaster() {
        return SecurityUtils.getCurrentMaster();
    }
}
