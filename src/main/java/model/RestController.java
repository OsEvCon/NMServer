package model;
import DAO.VisitDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@org.springframework.web.bind.annotation.RestController
public class RestController {
    private MasterRepository masterRepository;
    private ClientRepository clientRepository;
    private VisitRepository visitRepository;
    private ProcedureRepository procedureRepository;

    @Autowired
    public RestController(MasterRepository masterRepository, ClientRepository clientRepository,
                          VisitRepository visitRepository, ProcedureRepository procedureRepository) {
        this.masterRepository = masterRepository;
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
        this.procedureRepository = procedureRepository;
    }

    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }
    @GetMapping("/getClientsByMasterId")
    public List<Client> getClientsByMasterId(@RequestParam("masterId") int masterId){
        System.out.println("запрос на клиентов");
        return masterRepository.findMasterById(masterId).get().getClients();
    }

    @GetMapping("/getVisitsByMasterId")
    public List<Visit> getVisitsByMasterId(@RequestParam("masterId") int masterId){
        System.out.println("Запрос визитов");
        return visitRepository.findVisitsByMasterId(masterId).get();
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
        } else {
            result = "badRequest";
        }
        return result;
    }

    @PostMapping("/createVisit")
    public String createVisit(@RequestBody Visit visit, @RequestParam ("masterId") Integer masterId){
        System.out.println("запрос на добавление visit");
        String result;

        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);

        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();
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
            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }

    @PutMapping("/updateVisit")
    public String updateVisit(@RequestBody VisitDao visitDao, @RequestParam ("masterId") Integer masterId){
        System.out.println("Запрос на обновление Visit");
        String result = "badRequest";
        Optional<Visit> optionalVisit = visitRepository.findById(visitDao.getVisitId());
        if (optionalVisit.isPresent()){
            Visit visit = optionalVisit.get();
            visit.setClient(clientRepository.findClientById(visitDao.getClientId()).get());
            visit.setVisitDateTime(visitDao.getLocalDateTime());
            visit.getProcedures().clear();
            for (Integer i : visitDao.getProceduresId()){
                Procedure procedure = procedureRepository.findById(i).get();
                visit.getProcedures().add(procedure);
            }
            visitRepository.save(visit);
            result = "ok";
        }

        return result;
    }

    @PostMapping ("/deleteVisit")
    public String deleteVisits(@RequestBody List<Visit> visits, @RequestParam ("masterId") Integer masterId){
        System.out.println("запрос на удаление visit");
        String result;

        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);

        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();

            //Удаление visits из коллекции мастера
            master.getVisits().removeAll(visits);
            masterRepository.save(master);

            //Выставление в null клиентов в удаляемых визитах
            for (Visit visit : visits){
                visit.setClient(null);
                visit.setProcedures(null);
            }
            visitRepository.deleteAll(visits);
            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }

    @GetMapping("/getProceduresByMasterId")
    public List<Procedure> getProceduresByMasterId(@RequestParam("masterId") int masterId){
        System.out.println("Запрос процедур");
        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);
        List<Procedure> result = null;

        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();
            result = master.getProcedures();
        }
        return result;
    }

    @PostMapping ("/createProcedure")
    public String createProcedure(@RequestBody Procedure procedure, @RequestParam("masterId") Integer masterId){
        System.out.println("запрос на добавление procedure");
        String result;

        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);

        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();
            Procedure savedProcedure = procedureRepository.save(procedure);
            master.getProcedures().add(savedProcedure);
            masterRepository.save(master);
            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }

    @PostMapping("/deleteProcedures")
    public String deleteProcedures(@RequestBody List<Procedure> procedures, @RequestParam ("masterId") Integer masterId ){
        System.out.println("Запрос на удаление процедур");
        String result;

        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);

        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();
            master.getProcedures().removeAll(procedures);
            masterRepository.save(master);
            procedureRepository.deleteAll(procedures);
            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }
}
