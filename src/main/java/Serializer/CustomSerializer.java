package Serializer;

import Service.CryptoService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class CustomSerializer extends JsonSerializer<String> {
    private final CryptoService cryptoService;

    public CustomSerializer(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // Расшифровываем поле перед сериализацией
        String decryptedName = cryptoService.decrypt(value);
        gen.writeString(decryptedName);
    }
}
