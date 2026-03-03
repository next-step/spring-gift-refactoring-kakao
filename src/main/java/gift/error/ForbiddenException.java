package gift.error;

/* 접근 권한이 없을 때 발생하는 예외 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
