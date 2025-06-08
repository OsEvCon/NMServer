package controllers;

import Service.SecurityUtils;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class ClientController {

    private final MasterRepository masterRepository;
    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;
    private final SimpMessagingTemplate messagingTemplate;
@Autowired
    public ClientController(MasterRepository masterRepository, ClientRepository clientRepository, VisitRepository visitRepository, SimpMessagingTemplate messagingTemplate) {
        this.masterRepository = masterRepository;
    this.clientRepository = clientRepository;
    this.visitRepository = visitRepository;
    this.messagingTemplate = messagingTemplate;
}

    @GetMapping("/getClients")
    public List<Client> getClients(){
        System.out.println("запрос на клиентов");
        return getCurrentMaster().getClients();
    }

    @PostMapping("/createClient")
    public String saveClient(@RequestBody Client client){
        System.out.println("запрос на добавление клиента");
        String result;
        Master master = getCurrentMaster();
        if (master == null){
            // Отправить ответ, что такого мастера нет
            result = "badRequest";
        } else {
            clientRepository.save(client);

            List<Client> clients = master.getClients();
            clients.add(client);
            master.setClients(clients);
            masterRepository.save(master);
            result = "ok";

            messagingTemplate.convertAndSend("/topic/clients.update",
                    Map.of(
                            "type", "CREATED",
                            "client", client
                    ));
        }
        return result;
    }

    @PostMapping("/deleteClients")
    @Transactional
    public String deleteClients(@RequestBody List<Client> clients) {
        System.out.println("запрос на удаление клиентов");
        String result;
        Master master = getCurrentMaster();
        if (master == null){
            result = "badRequest";
        } else {
            // Удаление клиентов из коллекции клиентов у мастера
            master.getClients().removeAll(clients);

            //Обновление всех визитов, связанных с удаляемыми клиентами
            for (Client client : clients){
                List<Visit> visits = visitRepository.findVisitsByClient(client).get();
                for (Visit visit : visits) {
                    visit.setClient(null);
                }
                visitRepository.saveAll(visits); // Сохраняем изменения в визитах
            }
            // Сохранение изменений у мастера
            masterRepository.save(master);
            //удаление клиентов
            clientRepository.deleteAll(clients);

            result = "ok";

            messagingTemplate.convertAndSend("/topic/clients.update",
                    Map.of(
                            "type", "DELETED",
                            "clients", clients
                    ));
        }
        return result;
    }

    @PostMapping("/updateClient")
    public String updateClient(@RequestBody Client client){
        System.out.println("Запрос на обновление Client");
        String result = "badRequest";

        Optional<Client> optionalClient = clientRepository.findClientById(client.getId());

        if (optionalClient.isPresent()){
            Client clientForUpdate = optionalClient.get();
            clientForUpdate.setName(client.getName());
            clientForUpdate.setPhoneNumber(client.getPhoneNumber());
            clientRepository.save(clientForUpdate);
            result = "ok";

            messagingTemplate.convertAndSend("/topic/clients.update",
                    Map.of(
                            "type", "UPDATED",
                            "client", client
                    ));
        }
        return result;
    }

    private Master getCurrentMaster() {
        return SecurityUtils.getCurrentMaster();
    }
}
