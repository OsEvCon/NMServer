package Service;

import model.Master;
import model.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private MasterRepository masterRepository;
    @Override
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        Optional<Master> optionalMaster = masterRepository.findByEmail(userEmail);
        Master master;
        if (optionalMaster.isPresent()){
            master = optionalMaster.get();
        } else {throw new UsernameNotFoundException("Пользователь с таким email не найден " + userEmail);}

        return new User(master.getEmail(), master.getPassword(), master.getAuthorities());
    }

}
