package gift.auth;

public class MissingAuthorizationException extends RuntimeException {

    public MissingAuthorizationException(String message) {
        super(message);
    }
}
