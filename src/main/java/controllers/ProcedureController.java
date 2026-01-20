package controllers;

import model.Master;
import model.MasterRepository;
import model.Procedure;
import model.ProcedureRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import service.SecurityService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class ProcedureController {
    private final MasterRepository masterRepository;
    private final ProcedureRepository procedureRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityService securityService;

    public ProcedureController(MasterRepository masterRepository, ProcedureRepository procedureRepository, SimpMessagingTemplate messagingTemplate, SecurityService securityService) {
        this.masterRepository = masterRepository;
        this.procedureRepository = procedureRepository;
        this.messagingTemplate = messagingTemplate;
        this.securityService = securityService;
    }

    @GetMapping("/getProcedures")
    public List<Procedure> getProcedures(){
        System.out.println("Запрос процедур");
        Master master = securityService.getCurrentMasterOrThrow();
        List<Procedure> result = null;
        if (master != null){
            result = master.getProcedures();
        } else {
            result = null;
        }
        return result;
    }

    @PostMapping("/createProcedure")
    public String createProcedure(@RequestBody Procedure procedure){
        System.out.println("запрос на добавление procedure");
        String result;

        Master master = securityService.getCurrentMasterOrThrow();

        if (master != null){
            Procedure savedProcedure = procedureRepository.save(procedure);
            master.getProcedures().add(savedProcedure);
            masterRepository.save(master);
            result = "ok";

            messagingTemplate.convertAndSend("/topic/procedures.update",
                    Map.of(
                            "type", "CREATED",
                            "procedure", savedProcedure
                    ));
        } else {
            result = "badRequest";
        }
        return result;
    }

    @PostMapping("/deleteProcedures")
    public String deleteProcedures(@RequestBody List<Procedure> procedures){
        System.out.println("Запрос на удаление процедур");
        String result;

        Master master = securityService.getCurrentMasterOrThrow();

        if (master != null){
            master.getProcedures().removeAll(procedures);
            masterRepository.save(master);
            procedureRepository.deleteAll(procedures);
            result = "ok";

            messagingTemplate.convertAndSend("/topic/procedures.update",
                    Map.of(
                            "type", "DELETED",
                            "procedures", procedures
                    ));
        } else {
            result = "badRequest";
        }
        return result;
    }

    @PostMapping("/updateProcedure")
    public String updateProcedure(@RequestBody Procedure procedure) {
        System.out.println("Запрос на обновление/изменение процедуры");
        String result = "badRequest";

        Optional<Procedure> optionalProcedure = procedureRepository.findById(procedure.getId());
        if (optionalProcedure.isPresent()){
            Procedure procedureForUpdate = optionalProcedure.get();
            procedureForUpdate.setName(procedure.getName());
            procedureForUpdate.setPrice(procedure.getPrice());
            procedureRepository.save(procedureForUpdate);
            result = "ok";

            messagingTemplate.convertAndSend("/topic/procedures.update",
                    Map.of(
                            "type", "UPDATED",
                            "procedure", procedure
                    ));
        }
        return result;
    }

}
