package Service;

import model.Role;
import model.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        createRoles();
    }

    private void createRoles() {
        if (roleRepository.count() == 0) { // Проверяем, существуют ли роли
            Role userRole = new Role("ROLE_USER");
            Role adminRole = new Role("ROLE_ADMIN");
            roleRepository.save(userRole);
            roleRepository.save(adminRole);
            System.out.println("Роли успешно созданы: ROLE_USER и ROLE_ADMIN");
        } else {
            System.out.println("Роли уже существуют, пропускаем создание.");
        }
    }
}
