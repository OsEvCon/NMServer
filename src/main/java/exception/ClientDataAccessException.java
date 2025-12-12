package exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение при ошибках доступа к данным клиентов
 * Автоматически мапится на HTTP 503 Service Unavailable
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ClientDataAccessException extends RuntimeException {

    public ClientDataAccessException(Throwable cause) {
        super("Не удалось получить данные клиентов. Попробуйте позже.", cause);
    }

    public ClientDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientDataAccessException(String message) {
        super(message);
    }
}
