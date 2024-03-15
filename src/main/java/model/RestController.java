package model;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.GregorianCalendar;
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



    @GetMapping("/getClientsByMasterId")
    public List<Client> getClientsByMasterId(@RequestParam("masterId") int masterId){
        return masterRepository.findMasterById(masterId).get().getClients();
    }

    @GetMapping("/getVisitsByMasterId")
    public List<Visit> getVisitsByMasterId(@RequestParam("masterId") int masterId){
        List<Visit> visits = masterRepository.findMasterById(masterId).get().visits;
        return visits;

    }

    @GetMapping("/getSchedulesByMasterId")
    public List<Schedule> getSchedulesByMasterId(@RequestParam("masterId") int masterId){
       //return ScheduleDaoImpl.init().getSchedulesByMasterId(masterId);
        return null;
    }

    @PostMapping("/createClient")
    public ResponseEntity<Void> saveClient(@RequestBody Client client, @RequestParam("masterId") Integer masterId){
        ResponseEntity<Void> result;
        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);
        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();

            clientRepository.save(client);

            List<Client> clients = master.getClients();
            clients.add(client);
            master.setClients(clients);
            masterRepository.save(master);
            result = ResponseEntity.status(HttpStatus.OK).build();
        }
        else {
            // Отправить ответ, что такого мастера нет
            result = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return result;
    }
}
