package model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    private MasterRepository masterRepository;
    private ClientRepository clientRepository;

    private VisitRepository visitRepository;
    private ScheduleRepository scheduleRepository;
    private VisitDayRepository visitDayRepository;


    @Autowired
    public RestController(MasterRepository masterRepository, ClientRepository clientRepository,
                          VisitRepository visitRepository, ScheduleRepository scheduleRepository,
                          VisitDayRepository visitDayRepository) {
        this.masterRepository = masterRepository;
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
        this.scheduleRepository = scheduleRepository;
        this.visitDayRepository = visitDayRepository;
    }



    @GetMapping("/hello")
    public String hello(){
        System.out.println("запрос hello");
        return "hello";
    }

    @GetMapping("/getClientsByMasterId")
    public List<Client> getClientsByMasterId(@RequestParam("masterId") int masterId){
        System.out.println("запрос на клиентов");
        List<Client> clients = masterRepository.findMasterById(masterId).get().getClients();
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
            clientRepository.deleteAll(clients);

            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }

    @PostMapping("/createSchedule")
    public String createSchedule(@RequestBody Schedule schedule) {
        System.out.println("запрос на сохранение расписания");
        List<VisitDay> visitDays = schedule.getVisitDays();
        // Сохраняем расписание
        Schedule savedSchedule = scheduleRepository.save(schedule);

        // Получаем идентификатор сохраненного расписания
        int scheduleId = savedSchedule.getId();

        // Сохраняем все даты посещения, связанные с расписанием
        for (VisitDay visitDate : visitDays) {
            visitDate.setScheduleId(scheduleId);
            visitDayRepository.save(visitDate);
        }

        return "ok";
    }

    @PostMapping("/deleteSchedules")
    @Transactional
    public String deleteSchedules(@RequestBody List<Schedule> schedules, @RequestParam("masterId") int masterId){
        System.out.println("запрос удаления расписания");
        String result;
        Optional<Master> optionalMaster = masterRepository.findMasterById(masterId);

        if (optionalMaster.isPresent()){
            Master master = optionalMaster.get();
            for (Schedule schedule : schedules){
                visitDayRepository.deleteAll(schedule.getVisitDays());
            }

            scheduleRepository.deleteAll(schedules);

            result = "ok";
        } else {
            result = "badRequest";
        }
        return result;
    }




    @GetMapping("/getVisitDaysByMasterId")
    public List<VisitDay> getVisitDayByMasterId (@RequestParam ("masterId") int masterId ) {
        System.out.println("запрос visitDays");
        List<VisitDay> result = new ArrayList<>();

        for (Schedule schedule : scheduleRepository.getSchedulesByMasterId(masterId).get()) {
            result.addAll(schedule.getVisitDays());
        }
        return result;
    }

}
