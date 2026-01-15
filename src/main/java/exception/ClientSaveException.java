package exception;

/**
 * Исключение при ошибке сохранения клиента
 */
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
