package Service;

import model.Master;
import model.MasterRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {
    private static MasterRepository masterRepository;

    public static void init(MasterRepository repository){
        masterRepository = repository;
    }

    public static Master getCurrentMaster() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userEmail = userDetails.getUsername();

        return masterRepository.findByEmail(userEmail).orElse(null);
    }
}
