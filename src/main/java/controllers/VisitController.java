package controllers;

import DTO.VisitDTO;
import DTO.request.CreateVisitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import service.SecurityUtils;
import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import service.VisitService;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/visits")
@Slf4j
@RequiredArgsConstructor
public class VisitController {
    private final VisitRepository visitRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MasterRepository masterRepository;
    private final ClientRepository clientRepository;
    private final ProcedureRepository procedureRepository;
    private final VisitService visitService;

    @GetMapping()
    public ResponseEntity<List<VisitDTO>> getVisits(){
        log.debug("Запрос списка визитов");

        List<VisitDTO> visits = visitService.getVisits();

        return ResponseEntity.ok(visits);
    }

    @PostMapping()
    public ResponseEntity<VisitDTO> createVisit(@RequestBody @Valid CreateVisitRequest request){
        log.debug("Запрос на создание визита {}", request.getVisitDate());

        VisitDTO createdVisit = visitService.createVisit(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdVisit.getVisitId())
                .toUri();

        return ResponseEntity.created(location).body(createdVisit);
    }

    @Transactional(
            rollbackFor = {Exception.class}, // Откат при любых исключениях
            timeout = 5 // Максимальное время выполнения
    )
    @PutMapping("/updateVisitTest")
    public ResponseEntity<Visit> updateVisit(@RequestBody VisitDTO visitDTO){
        System.out.println("Запрос на обновление Visit");

        Optional<Visit> optionalVisit = visitRepository.findById(visitDTO.getVisitId());
        if (optionalVisit.isPresent()){
            Visit visit = optionalVisit.get();
            if (visitDTO.getClientId() == null){
                visit.setClient(null);
            } else {
                visit.setClient(clientRepository.findClientById(visitDTO.getClientId()).get());
            }
            visit.setVisitDateTime(visitDTO.getVisitDateTime());
            visit.getProcedures().clear();
            for (Integer i : visitDTO.getProceduresId()){
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
            return ResponseEntity.badRequest().body(visitRepository.findById(visitDTO.getVisitId()).get());
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
