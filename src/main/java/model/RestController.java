package model;
import DAO.VisitDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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



    // Метод для получения email текущего аутентифицированного пользователя
    private Master getCurrentMaster() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        if (authentication != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userEmail = userDetails.getUsername(); // Предполагается, что email - это username
        }
        Optional<Master> optionalMaster = masterRepository.findByEmail(userEmail);
        return optionalMaster.orElse(null);
    }

}
