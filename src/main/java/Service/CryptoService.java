package Service;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CryptoService {
    private static final String ALGORITHM = "AES";
    private static SecretKey secretKey;
    private static String filePath = "keys/secretKey.key";

    static {
        try {
            secretKey = loadKeyFromFile(filePath);
            if (secretKey == null) {
                secretKey = generateKey();
                saveKeyToFile(secretKey, filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SecretKey loadKeyFromFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        byte[] keyBytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(filePath)) {
            fis.read(keyBytes);
        }

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(128); // или 192, 256 бит
        return keyGen.generateKey();
    }

    private static void saveKeyToFile(SecretKey secretKey, String filePath) {
        // Создаем объект File для указанного пути
        File file = new File(filePath);

        // Получаем родительскую директорию
        File parentDir = file.getParentFile();

        // Проверяем, существует ли директория, если нет — создаем ее
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Создает все отсутствующие директории
        }

        // Записываем ключ в файл
        byte[] keyBytes = secretKey.getEncoded();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(keyBytes);
        } catch (IOException e) {
            System.out.println("Ошибка сохранения ключа" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String encrypt(String data)  {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException("Ошибка кодирования данных" + e.getMessage());
        }
    }

    public String decrypt(String encryptedData)  {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException("Ошибка декодирования данных" + e.getMessage());
        }
    }
}
