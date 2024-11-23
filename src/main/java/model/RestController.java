package model;
import DAO.VisitDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
    @GetMapping("/getClients")
    public List<Client> getClients(){
        System.out.println("запрос на клиентов");
        return getCurrentMaster().getClients();
    }

    @GetMapping("/getVisits")
    public List<Visit> getVisits(){
        System.out.println("Запрос визитов от пользователя " + getCurrentMaster().getEmail());
        return getCurrentMaster().visits;
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
            }
        return result;
    }

    @PostMapping("/createVisit")
    public String createVisit(@RequestBody Visit visit){
        System.out.println("запрос на добавление visit");
        String result;
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
                result = "ok";
            } else {
                result = "badRequest";
            }
        return result;
    }

    @PutMapping("/updateVisit")
    public String updateVisit(@RequestBody VisitDao visitDao){
        System.out.println("Запрос на обновление Visit");
        String result = "badRequest";
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
            result = "ok";
        }
        return result;
    }

    @PostMapping ("/deleteVisit")
    public String deleteVisits(@RequestBody List<Visit> visits){
        System.out.println("запрос на удаление visit");
        String result;

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
                result = "ok";
            } else {
                result = "badRequest";
            }
        return result;
    }

    @GetMapping("/getProcedures")
    public List<Procedure> getProcedures(){
        System.out.println("Запрос процедур");
        Master master = getCurrentMaster();
        List<Procedure> result = null;

        if (master != null){
            result = master.getProcedures();
        } else {
            result = null;
        }
        return result;
    }

    @PostMapping ("/createProcedure")
    public String createProcedure(@RequestBody Procedure procedure){
        System.out.println("запрос на добавление procedure");
        String result;

        Master master = getCurrentMaster();

        if (master != null){
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
    public String deleteProcedures(@RequestBody List<Procedure> procedures){
        System.out.println("Запрос на удаление процедур");
        String result;

        Master master = getCurrentMaster();

        if (master != null){
            master.getProcedures().removeAll(procedures);
            masterRepository.save(master);
            procedureRepository.deleteAll(procedures);
            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }

    // Метод для получения email текущего аутентифицированного пользователя
    private Master getCurrentMaster() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("второй   " + authentication.getPrincipal().toString());
        String userEmail = null;
        if (authentication != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userEmail = userDetails.getUsername(); // Предполагается, что email - это username
        }
        Optional<Master> optionalMaster = masterRepository.findByEmail(userEmail);
        return optionalMaster.orElse(null);
    }

}
