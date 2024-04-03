package model;
import org.apache.juli.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    private MasterRepository masterRepository;
    private ClientRepository clientRepository;

    private VisitRepository visitRepository;


    @Autowired
    public RestController(MasterRepository masterRepository, ClientRepository clientRepository, VisitRepository visitRepository) {
        this.masterRepository = masterRepository;
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
    }



    @GetMapping("/hello")
    public String hello(){
        System.out.println("запрос hello");
        return "hello";
    }

    @GetMapping("/getClientsByMasterId")
    public List<Client> getClientsByMasterId(@RequestParam("masterId") int masterId){
        System.out.println("запрос на клиентов");
        return masterRepository.findMasterById(masterId).get().getClients();
    }

    @GetMapping("/getVisitsByMasterId")
    public List<Visit> getVisitsByMasterId(@RequestParam("masterId") int masterId){
        return masterRepository.findMasterById(masterId).get().visits;
    }

    @GetMapping("/getSchedulesByMasterId")
    public List<Schedule> getSchedulesByMasterId(@RequestParam("masterId") int masterId){
        System.out.println("запрос расписания");
        return masterRepository.findMasterById(masterId).get().schedules;
    }

    @PostMapping("/createClient")
    public String saveClient(@RequestBody Client client, @RequestParam("masterId") Integer masterId){
        String result;
        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);
        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();

            clientRepository.save(client);

            List<Client> clients = master.getClients();
            clients.add(client);
            master.setClients(clients);
            masterRepository.save(master);
            result = "ok";
        }
        else {
            // Отправить ответ, что такого мастера нет
            result = "badRequest";
        }
        return result;
    }

    @PostMapping("/deleteClients")
    @Transactional
    public String deleteClients(@RequestBody List<Client> clients, @RequestParam("masterId") Integer masterId) {
        System.out.println("запрос на удаление клиентов");
        String result;

        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);
        if (optionalMaster.isPresent()) {
            Master master = optionalMaster.get();

            // Удаление клиентов из коллекции клиентов у мастера
            master.getClients().removeAll(clients);

            // Сохранение изменений
            masterRepository.save(master);

            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }




}
