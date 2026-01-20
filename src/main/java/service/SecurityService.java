package service;

import model.Master;
import model.MasterRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private final MasterRepository masterRepository;

    public SecurityService(MasterRepository masterRepository) {
        this.masterRepository = masterRepository;
    }

    /**
     * Получение текущего аутентифицированного мастера
     * @return текущий мастер или null если не аутентифицирован
     */
    public Master getCurrentMaster() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userEmail = userDetails.getUsername();

        return masterRepository.findByEmail(userEmail).orElse(null);
    }

    /**
     * Получение текущего мастера с проверкой (рекомендуется использовать этот метод)
     * @return текущий аутентифицированный мастер
     * @throws IllegalStateException если мастер не аутентифицирован или не найден
     */
    public Master getCurrentMasterOrThrow() {
        Master master = getCurrentMaster();
        if (master == null) {
            throw new IllegalStateException("Текущий мастер не аутентифицирован или не найден");
        }
        return master;
    }

    /**
     * Получение email текущего пользователя
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Проверка аутентификации
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
