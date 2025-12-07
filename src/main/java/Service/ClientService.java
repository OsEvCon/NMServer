package Service;

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
public class ClientService {
    private final MasterRepository masterRepository;
    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ClientService(MasterRepository masterRepository, ClientRepository clientRepository, VisitRepository visitRepository, SimpMessagingTemplate messagingTemplate) {
        this.masterRepository = masterRepository;
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
        this.messagingTemplate = messagingTemplate;
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
