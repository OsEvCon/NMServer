package exception;

public class VisitDataAccessException extends RuntimeException{
    public VisitDataAccessException(String message) {
        super(message);
    }

    public VisitDataAccessException(Throwable cause) {
        super("Не удалось получить данные визитов. Попробуйте позже", cause);
    }

    public VisitDataAccessException(String message, Throwable cause) {}
}
