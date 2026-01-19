package exception;

public class UpdateException extends BusinessException {
    public UpdateException(String message) {
        super("Ошибка обновления. " + message);
    }
}
