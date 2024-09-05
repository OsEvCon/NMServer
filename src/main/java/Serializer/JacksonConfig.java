package Serializer;

import Service.CryptoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public CryptoService cryptoService() {
        return new CryptoService(); // сервис шифрования
    }
}
