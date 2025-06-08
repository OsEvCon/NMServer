package controllers;

import DAO.VisitDao;
import Service.SecurityUtils;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class VisitController {
    private final VisitRepository visitRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MasterRepository masterRepository;
    private final ClientRepository clientRepository;
    private final ProcedureRepository procedureRepository;
@Autowired
    public VisitController(VisitRepository visitRepository, SimpMessagingTemplate messagingTemplate, MasterRepository masterRepository, ClientRepository clientRepository, ProcedureRepository procedureRepository) {
        this.visitRepository = visitRepository;
        this.messagingTemplate = messagingTemplate;
    this.masterRepository = masterRepository;
    this.clientRepository = clientRepository;
    this.procedureRepository = procedureRepository;
}

    @GetMapping("/getVisits")
    public List<Visit> getVisits(){
        System.out.println("Запрос визитов от пользователя " + getCurrentMaster().getEmail());
        return getCurrentMaster().getVisits();
    }

    @PostMapping("/createVisitTest")
    public ResponseEntity<Visit> createVisit(@RequestBody Visit visit){
        System.out.println("запрос на добавление visit");
        Master master = getCurrentMaster();
        if (master != null){
            visit.setMaster(master);
            Visit savedVisit = visitRepository.save(visit);
            master.getVisits().add(savedVisit);
            masterRepository.save(master);

            // Если клиент присутствует, добавляем его в базу данных
            if (visit.getClient() != null) {
                Optional<Client> optionalClient = clientRepository.findClientById(visit.getClient().getId());
                if (optionalClient.isPresent()) {
                    Client client = optionalClient.get();
                    client.getVisits().add(savedVisit);
                    clientRepository.save(client);
                }
            }

            messagingTemplate.convertAndSend("/topic/visits.update",
                    Map.of(
                            "type", "CREATED",
                            "visit", savedVisit
                    ));
            return ResponseEntity.ok(savedVisit);
        } else {
            return ResponseEntity.badRequest().body(visit);
        }
    }

    @Transactional(
            rollbackFor = {Exception.class}, // Откат при любых исключениях
            timeout = 5 // Максимальное время выполнения
    )
    @PutMapping("/updateVisitTest")
    public ResponseEntity<Visit> updateVisit(@RequestBody VisitDao visitDao){
        System.out.println("Запрос на обновление Visit");

        Optional<Visit> optionalVisit = visitRepository.findById(visitDao.getVisitId());
        if (optionalVisit.isPresent()){
            Visit visit = optionalVisit.get();
            if (visitDao.getClientId() == null){
                visit.setClient(null);
            } else {
                visit.setClient(clientRepository.findClientById(visitDao.getClientId()).get());
            }
            visit.setVisitDateTime(visitDao.getLocalDateTime());
            visit.getProcedures().clear();
            for (Integer i : visitDao.getProceduresId()){
                Procedure procedure = procedureRepository.findById(i).get();
                visit.getProcedures().add(procedure);
            }
            visitRepository.save(visit);

            messagingTemplate.convertAndSend("/topic/visits.update",
                    Map.of("type", "UPDATED",
                            "visit", visit
                    ));
            return ResponseEntity.ok(visit);
        } else {
            return ResponseEntity.badRequest().body(visitRepository.findById(visitDao.getVisitId()).get());
        }
    }

    @PostMapping ("/deleteVisitTest")
    public ResponseEntity<List<Visit>> deleteVisits(@RequestBody List<Visit> visits){
        System.out.println("запрос на удаление нескольких визитов");

        Master master = getCurrentMaster();
        if (master != null){
            //Удаление visits из коллекции мастера
            master.getVisits().removeAll(visits);
            masterRepository.save(master);

            //Выставление в null клиентов в удаляемых визитах
            for (Visit visit : visits){
                visit.setClient(null);
                visit.setProcedures(null);
            }
            visitRepository.deleteAll(visits);

            messagingTemplate.convertAndSend("/topic/visits.update",
                    Map.of("type", "DELETED",
                            "visits", visits
                    ));

            return ResponseEntity.ok(visits);
        } else {
            return ResponseEntity.badRequest().body(visits);
        }
    }

    private Master getCurrentMaster() {
        return SecurityUtils.getCurrentMaster();
    }
}
