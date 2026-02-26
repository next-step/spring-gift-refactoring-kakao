package gift.error;

/* 인증 실패 시 발생하는 예외 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
