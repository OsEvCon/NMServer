package Serializer;

import Service.CryptoService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class CustomDeserializer extends JsonDeserializer<String> {
    private final CryptoService cryptoService;

    public CustomDeserializer(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Получаем значение поля
        String name = p.getText();
        // Шифруем его перед десериализацией
        return cryptoService.encrypt(name);
    }
}
