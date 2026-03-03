package gift.error;

/* 공통 에러 코드 열거형 */
public enum ErrorCode {
    INVALID_REQUEST("잘못된 요청입니다."),
    NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),
    UNAUTHORIZED("인증에 실패했습니다."),
    FORBIDDEN("접근 권한이 없습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
