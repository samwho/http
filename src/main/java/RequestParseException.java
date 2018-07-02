public class RequestParseException extends Exception {
    public RequestParseException(Throwable cause) {
        super(cause);
    }

    public RequestParseException(String message) {
        super(message);
    }

    public RequestParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
