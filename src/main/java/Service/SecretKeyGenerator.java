package Service;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class SecretKeyGenerator {
    // Конфигурация
    private static final String ALGORITHM = "AES";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // 256 бит для AES-256

    /**
     * Генерирует SecretKey на основе email пользователя
     * @param email Email пользователя (будет использован как основа для ключа)
     * @param salt Уникальная соль (должна храниться безопасно)
     * @return SecretKey для AES-шифрования
     */
    public static SecretKey generateKeyFromEmail(String email, String salt) {
        try {
            // 1. Создаем KeySpec с использованием PBKDF2
            KeySpec spec = new PBEKeySpec(
                    email.toCharArray(),
                    salt.getBytes(),
                    ITERATIONS,
                    KEY_LENGTH
            );

            // 2. Генерируем промежуточный ключ
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();

            // 3. Создаем AES ключ
            return new SecretKeySpec(derivedKey, ALGORITHM);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Ошибка генерации ключа", e);
        }
    }

    /**
     * Восстанавливает SecretKey из Base64 строки
     */
    public static SecretKey restoreKeyFromString(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }

    /**
     * Конвертирует SecretKey в Base64 строку
     */
    public static String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
