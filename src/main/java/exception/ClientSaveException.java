package exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение при ошибке сохранения клиента
 * Автоматически мапится на HTTP 503 Service Unavailable
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ClientSaveException extends RuntimeException{

    public ClientSaveException(String message){
        super(message);
    }

    public ClientSaveException(String message, Throwable cause){
        super(message,cause);
    }

    public ClientSaveException(Throwable cause){
        super("Не удалось сохранить клиента", cause);
    }

    public ClientSaveException() {
        super("Не удалось сохранить клиента");
    }
}
